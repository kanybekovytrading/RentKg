package kg.rental.service;

import kg.rental.bot.RentalBot;
import kg.rental.config.TelegramConfig;
import kg.rental.entity.Listing;
import kg.rental.enums.ListingStatus;
import kg.rental.enums.ListingType;
import kg.rental.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramChannelService {

    private final TelegramConfig config;
    private final MessageHelper messageHelper;
    private final RentalBot rentalBot;
    private final ListingRepository listingRepository;

    public int publishListing(Listing listing) {
        String text = messageHelper.formatListing(listing);
        InlineKeyboardMarkup keyboard = buildComplaintKeyboard(listing.getId());
        Integer threadId = getThreadId(listing.getType());
        return sendToThread(threadId, text, keyboard, listing);
    }

    public void updateListingStatus(Listing listing) {
        if (listing.getMainChannelMsgId() == null) return;

        // Перечитываем актуальный статус из БД
        listing = listingRepository.findById(listing.getId()).orElse(listing);

        String text = messageHelper.formatListing(listing);

        if (listing.getStatus() == ListingStatus.CLOSED) {
            text = "🔴 <b>УЖЕ НЕАКТУАЛЬНО</b>\n\n" + text;
        } else if (listing.getStatus() == ListingStatus.ARCHIVED) {
            text = "⛔ <b>ЗАБЛОКИРОВАНО</b>\n\n" + text;
        }

        InlineKeyboardMarkup keyboard = buildComplaintKeyboard(listing.getId());
        editMessage(listing.getMainChannelMsgId(), text, keyboard);
    }

    @Transactional
    public void publishBlacklistWarning(Listing listing) {
        listing = listingRepository.findById(listing.getId()).orElse(listing);

        String text = messageHelper.formatBlacklistWarning(listing);
        String[] photos = listing.getPhotoFileIds();

        try {
            if (photos != null && photos.length >= 2) {
                // Медиагруппа с текстом в первом фото
                List<InputMedia> media = new ArrayList<>();
                for (int i = 0; i < photos.length; i++) {
                    InputMediaPhoto p = new InputMediaPhoto(photos[i]);
                    if (i == 0) { p.setCaption(text); p.setParseMode("HTML"); }
                    media.add(p);
                }
                rentalBot.execute(SendMediaGroup.builder()
                        .chatId(config.getMainChannel())
                        .messageThreadId(config.getThreadBlacklist())
                        .medias(media)
                        .build());

            } else if (photos != null && photos.length == 1) {
                rentalBot.execute(SendPhoto.builder()
                        .chatId(config.getMainChannel())
                        .messageThreadId(config.getThreadBlacklist())
                        .photo(new InputFile(photos[0]))
                        .caption(text)
                        .parseMode("HTML")
                        .build());

            } else {
                // Без фото
                sendTextToThread(config.getThreadBlacklist(), text, null);
            }
        } catch (TelegramApiException e) {
            log.error("Failed to publish blacklist warning: {}", e.getMessage());
        }
    }
    /** Обычное уведомление подписчику (по подписке на район/тип) */
    public void sendNotification(Long telegramId, Listing listing) {
        String text = messageHelper.formatNotification(listing);
        try {
            rentalBot.execute(SendMessage.builder()
                    .chatId(telegramId.toString())
                    .text(text)
                    .parseMode("HTML")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send notification to {}: {}", telegramId, e.getMessage());
        }
    }

    /**
     * Уведомление о совпадении (матчинге) — когда появилось объявление точно под критерии.
     * Отправляется пользователю чьё встречное объявление совпадает.
     */
    public void sendMatchNotification(Long telegramId, Listing matchedListing) {
        String header = switch (matchedListing.getType()) {
            case RENT_OUT        -> "🎯 Появилась квартира под ваши критерии!";
            case RENT_IN         -> "🎯 Появился арендатор под ваше объявление!";
            case ROOMMATE_OFFER  -> "🎯 Появился вариант подселения под ваши критерии!";
            case ROOMMATE_SEEK   -> "🎯 Появился желающий подселиться к вам!";
            case RENT_ROOM_IN    -> "🎯 Появилась комната под ваши критерии!";
        };

        String text = header + "\n\n" + messageHelper.formatListing(matchedListing);
        try {
            rentalBot.execute(SendMessage.builder()
                    .chatId(telegramId.toString())
                    .text(text)
                    .parseMode("HTML")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send match notification to {}: {}", telegramId, e.getMessage());
        }
    }

    public void sendReminder(Long telegramId, Long listingId) {
        String text = messageHelper.formatReminderMessage(listingId);
        InlineKeyboardMarkup keyboard = buildReminderKeyboard(listingId);
        try {
            rentalBot.execute(SendMessage.builder()
                    .chatId(telegramId.toString())
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send reminder to {}: {}", telegramId, e.getMessage());
        }
    }

    // ── private ──

    private int sendToThread(Integer threadId, String text,
                             InlineKeyboardMarkup keyboard, Listing listing) {
        try {
            String[] photos = listing.getPhotoFileIds();

            if (photos != null && photos.length >= 2) {
                List<InputMedia> media = new ArrayList<>();
                for (int i = 0; i < photos.length; i++) {
                    InputMediaPhoto p = new InputMediaPhoto(photos[i]);
                    if (i == 0) { p.setCaption(text); p.setParseMode("HTML"); }
                    media.add(p);
                }
                List<Message> sent = rentalBot.execute(SendMediaGroup.builder()
                        .chatId(config.getMainChannel())
                        .messageThreadId(threadId)
                        .medias(media)
                        .build());
                sendTextToThread(threadId, "⬆️ Объявление выше", keyboard);
                return sent.get(0).getMessageId();

            } else if (photos != null && photos.length == 1) {
                Message sent = rentalBot.execute(SendPhoto.builder()
                        .chatId(config.getMainChannel())
                        .messageThreadId(threadId)
                        .photo(new InputFile(photos[0]))
                        .caption(text)
                        .parseMode("HTML")
                        .replyMarkup(keyboard)
                        .build());
                return sent.getMessageId();

            } else {
                return sendTextToThread(threadId, text, keyboard);
            }
        } catch (TelegramApiException e) {
            log.error("Failed to publish to thread {}: {}", threadId, e.getMessage());
            return -1;
        }
    }

    private int sendTextToThread(Integer threadId, String text, InlineKeyboardMarkup keyboard) {
        try {
            SendMessage.SendMessageBuilder builder = SendMessage.builder()
                    .chatId(config.getMainChannel())
                    .messageThreadId(threadId)
                    .text(text)
                    .parseMode("HTML");
            if (keyboard != null) builder.replyMarkup(keyboard);
            return rentalBot.execute(builder.build()).getMessageId();
        } catch (TelegramApiException e) {
            log.error("Failed to send text to thread {}: {}", threadId, e.getMessage());
            return -1;
        }
    }

    private void editMessage(int messageId, String text, InlineKeyboardMarkup keyboard) {
        try {
            rentalBot.execute(EditMessageText.builder()
                    .chatId(config.getMainChannel())
                    .messageId(messageId)
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to edit message {}: {}", messageId, e.getMessage());
        }
    }

    private Integer getThreadId(ListingType type) {
        return switch (type) {
            case RENT_OUT        -> config.getThreadRentOut();
            case RENT_IN         -> config.getThreadRentIn();
            case RENT_ROOM_IN , ROOMMATE_SEEK   -> config.getThreadRoommate();
            case ROOMMATE_OFFER  -> config.getThreadNeedRoommate();
        };
    }

    private InlineKeyboardMarkup buildComplaintKeyboard(Long listingId) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("⚠️ Пожаловаться")
                                .callbackData("complaint:" + listingId)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("📝 Подать объявление")
                                .url("https://t.me/Bishkek_RentKg_bot")
                                .build()
                ))
                .build();
    }
    public void deleteListingFromChannel(Listing listing) {
        log.info("deleteListingFromChannel called, mainChannelMsgId={}", listing.getMainChannelMsgId());
        if (listing.getMainChannelMsgId() == null) {
            log.warn("mainChannelMsgId is NULL, cannot delete");
            return;
        }
        try {
            rentalBot.execute(DeleteMessage.builder()
                    .chatId(config.getMainChannel())
                    .messageId(listing.getMainChannelMsgId())
                    .build());
            log.info("Message deleted successfully");
        } catch (TelegramApiException e) {
            log.error("Failed to delete message {}: {}", listing.getMainChannelMsgId(), e.getMessage());
        }
    }

    private InlineKeyboardMarkup buildReminderKeyboard(Long listingId) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        InlineKeyboardButton.builder()
                                .text("✅ Да, актуально")
                                .callbackData("confirm:" + listingId)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("❌ Уже сдано")
                                .callbackData("close:" + listingId)
                                .build()
                ))
                .build();
    }
}