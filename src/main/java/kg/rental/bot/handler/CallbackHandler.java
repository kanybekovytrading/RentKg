package kg.rental.bot.handler;

import kg.rental.bot.Keyboards;
import kg.rental.bot.RentalBot;
import kg.rental.entity.Listing;
import kg.rental.entity.User;
import kg.rental.enums.ComplaintReason;
import kg.rental.enums.UserState;
import kg.rental.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallbackHandler {

    private final RentalBot bot;
    private final ListingService listingService;
    private final ComplaintService complaintService;
    private final TelegramChannelService channelService;
    private final Keyboards keyboards;
    private final UserService userService;

    public void handle(CallbackQuery callback) {
        try {
            String data = callback.getData();
            long telegramId = callback.getFrom().getId();
            String callbackId = callback.getId();
            Integer messageId = callback.getMessage().getMessageId();

            if (data.startsWith("complaint:")) {
                // Передаём from для автрегистрации
                userService.getOrCreate(callback.getFrom());
                handleComplaintStart(telegramId, Long.parseLong(data.split(":")[1]), callbackId);
            } else if (data.startsWith("complaint_reason:")) {
                // Тоже регистрируем на случай если первый шаг пропустили
                userService.getOrCreate(callback.getFrom());
                String[] p = data.split(":");
                handleComplaintReason(telegramId, Long.parseLong(p[1]), ComplaintReason.valueOf(p[2]), callbackId);

            } else if (data.startsWith("tenant_toggle:")) {
                handleTenantToggle(telegramId, data.split(":")[1], callbackId, messageId);
            } else if (data.equals("tenant_done")) {
                handleTenantDone(telegramId, callbackId);
            } else if (data.startsWith("seek_spots:")) {
                handleSeekSpots(telegramId, data.split(":")[1], callbackId);
            } else if (data.startsWith("offer_type:")) {
                handleOfferType(telegramId, data.split(":")[1], callbackId);
            } else if (data.startsWith("confirm:")) {
                handleConfirm(telegramId, Long.parseLong(data.split(":")[1]), callbackId);
            } else if (data.startsWith("close:")) {
                handleClose(telegramId, Long.parseLong(data.split(":")[1]), callbackId);
            } else if (data.startsWith("my_close:")) {
                handleMyClose(telegramId, Long.parseLong(data.split(":")[1]), callbackId, messageId);
            } else if (data.startsWith("my_extend:")) {
                handleMyExtend(telegramId, Long.parseLong(data.split(":")[1]), callbackId);
            } else if (data.startsWith("my_reopen:")) {
                handleMyReopen(telegramId, Long.parseLong(data.split(":")[1]), callbackId);
            } else if (data.equals("noop")) {
                answer(callbackId, "");
            }
        } catch (Exception e) {
                log.error("=== CALLBACK FATAL ERROR: {}", e.getMessage(), e);
            }
    }

    // ── Жалобы ──

    private void handleComplaintStart(Long telegramId, Long listingId, String callbackId) {
        // Автоматически регистрируем пользователя если не существует
        answer(callbackId, "Выберите причину");
        sendInline(telegramId, "⚠️ Жалоба на объявление #" + listingId + "\n\nВыберите причину:",
                keyboards.complaintReasons(listingId));
    }

    private void handleComplaintReason(Long telegramId, Long listingId,
                                       ComplaintReason reason, String callbackId) {
        log.info("=== handleComplaintReason: listingId={}, reporter={}, reason={}", listingId, telegramId, reason);

        boolean banned = complaintService.submitComplaint(listingId, telegramId, reason);

        log.info("=== submitComplaint result: banned={}", banned);

        if (banned) {
            listingService.findById(listingId).ifPresent(l -> {
                log.info("=== BANNING: listing={}, msgId={}", l.getId(), l.getMainChannelMsgId());
                try {
                    channelService.publishBlacklistWarning(l);
                    log.info("=== publishBlacklistWarning OK");
                } catch (Exception e) {
                    log.error("=== publishBlacklistWarning FAILED: {}", e.getMessage(), e);
                }
                try {
                    channelService.deleteListingFromChannel(l);
                    log.info("=== deleteListingFromChannel OK");
                } catch (Exception e) {
                    log.error("=== deleteListingFromChannel FAILED: {}", e.getMessage(), e);
                }
            });
            answer(callbackId, "🚫 Объявление заблокировано!");
            send(telegramId, "🚫 Объявление заблокировано. Контакт добавлен в чёрный список.");

         } else if (reason == ComplaintReason.ALREADY_RENTED) {
            // статус уже CLOSED после submitComplaint
            listingService.findById(listingId).ifPresent(channelService::updateListingStatus);
        answer(callbackId, "✅ Отмечено как сдано");
        send(telegramId, "✅ Объявление отмечено как уже сдано.");
    }else {
            answer(callbackId, "✅ Жалоба принята!");
            send(telegramId, "✅ Жалоба принята.");
        }
    }

    // ── Мультиселект "для кого сдаёшь" ──

    /**
     * Пользователь нажал на одну из опций — toggle в черновике, перерисовываем кнопки
     */
    @SuppressWarnings("unchecked")
    private void handleTenantToggle(Long telegramId, String value, String callbackId, Integer messageId) {
        User user = userService.getByTelegramId(telegramId);
        Map<String, Object> draft = userService.getDraft(user.getId());

        Set<String> selected = new LinkedHashSet<>(
                (List<String>) draft.getOrDefault("tenantTypes", new ArrayList<>())
        );

        // "ANY" — взаимоисключающий со всеми остальными
        if ("ANY".equals(value)) {
            selected.clear();
            selected.add("ANY");
        } else {
            selected.remove("ANY"); // снимаем "Всем" если выбирают конкретное
            if (selected.contains(value)) {
                selected.remove(value);
            } else {
                selected.add(value);
            }
        }

        userService.saveDraftField(user.getId(), "tenantTypes", new ArrayList<>(selected));
        answer(callbackId, "");

        // Перерисовываем инлайн-клавиатуру с новыми галочками
        editButtons(telegramId, messageId, keyboards.tenantTypeMultiInline(selected));
    }

    /**
     * Пользователь нажал "Готово" — сохраняем выбор и идём дальше
     */
    @SuppressWarnings("unchecked")
    private void handleTenantDone(Long telegramId, String callbackId) {
        User user = userService.getByTelegramId(telegramId);
        Map<String, Object> draft = userService.getDraft(user.getId());

        List<String> selected = (List<String>) draft.getOrDefault("tenantTypes", new ArrayList<>());
        if (selected.isEmpty()) {
            answer(callbackId, "Выберите хотя бы один вариант!");
            return;
        }

        // Для обратной совместимости сохраняем как строку через запятую
        userService.saveDraftField(user.getId(), "tenantType", String.join(",", selected));

        UserState state = user.getState();
        if (state == UserState.RENT_OUT_TENANT_TYPE) {
            userService.setState(telegramId, UserState.RENT_OUT_CONTACT);
            answer(callbackId, "✅");
            send(telegramId, "📞 Ваш контакт для связи (номер или @username)?");
        } else if (state == UserState.ROOMMATE_OFFER_GENDER) {
            userService.setState(telegramId, UserState.ROOMMATE_OFFER_AMENITIES);
            answer(callbackId, "✅");
            send(telegramId, "📝 Что есть в комнате? (напишите или 'Пропустить ⏭')",
                    keyboards.skipOrFinish());
        } else {
            answer(callbackId, "✅");
        }
    }

    private void handleSeekSpots(Long telegramId, String value, String callbackId) {
        User user = userService.getByTelegramId(telegramId);
        userService.saveDraftField(user.getId(), "spotsAvailable", Integer.parseInt(value));
        userService.setState(telegramId, UserState.ROOMMATE_SEEK_WHEN);
        answer(callbackId, "✅");
        send(telegramId, "📅 Когда нужно?", keyboards.when());
    }

    private void handleOfferType(Long telegramId, String value, String callbackId) {
        User user = userService.getByTelegramId(telegramId);
        userService.saveDraftField(user.getId(), "offerRoomType", value);
        userService.setState(telegramId, UserState.ROOMMATE_OFFER_DISTRICT);
        answer(callbackId, "✅");
        send(telegramId, "📍 В каком районе?");
    }

    private void handleConfirm(Long telegramId, Long listingId, String callbackId) {
        Optional<Listing> opt = listingService.findById(listingId);
        if (opt.isEmpty() || !opt.get().getUser().getTelegramId().equals(telegramId)) {
            answer(callbackId, "Ошибка"); return;
        }
        listingService.confirmListing(listingId);
        channelService.updateListingStatus(opt.get());
        answer(callbackId, "✅ Обновлено");
        send(telegramId, "✅ Объявление #" + listingId + " актуально 🟢");
    }

    private void handleClose(Long telegramId, Long listingId, String callbackId) {
        Optional<Listing> opt = listingService.findById(listingId);
        if (opt.isEmpty() || !opt.get().getUser().getTelegramId().equals(telegramId)) {
            answer(callbackId, "Ошибка"); return;
        }
        listingService.closeListing(listingId);
        channelService.updateListingStatus(opt.get());
        answer(callbackId, "Закрыто");
        send(telegramId, "✅ Объявление #" + listingId + " закрыто. Удачи! 🎉");
    }

    // ── Мои объявления ──

    private void handleMyClose(Long telegramId, Long listingId, String callbackId, Integer messageId) {
        Optional<Listing> opt = listingService.findById(listingId);
        if (opt.isEmpty() || !opt.get().getUser().getTelegramId().equals(telegramId)) {
            answer(callbackId, "Ошибка"); return;
        }
        listingService.closeListing(listingId);
        channelService.updateListingStatus(opt.get());
        answer(callbackId, "✅ Закрыто");
        editButtons(telegramId, messageId, keyboards.myListingActions(listingId, opt.get().getStatus()));
    }

    private void handleMyExtend(Long telegramId, Long listingId, String callbackId) {
        Optional<Listing> opt = listingService.findById(listingId);
        if (opt.isEmpty() || !opt.get().getUser().getTelegramId().equals(telegramId)) {
            answer(callbackId, "Ошибка"); return;
        }
        listingService.confirmListing(listingId);
        channelService.updateListingStatus(opt.get());
        answer(callbackId, "✅ Продлено на 7 дней");
    }

    private void handleMyReopen(Long telegramId, Long listingId, String callbackId) {
        Optional<Listing> opt = listingService.findById(listingId);
        if (opt.isEmpty() || !opt.get().getUser().getTelegramId().equals(telegramId)) {
            answer(callbackId, "Ошибка"); return;
        }
        listingService.confirmListing(listingId);
        int msgId = channelService.publishListing(opt.get());
        listingService.saveChannelMessageId(listingId, msgId);
        answer(callbackId, "✅ Переопубликовано");
        send(telegramId, "✅ Объявление #" + listingId + " снова опубликовано!");
    }

    // ── helpers ──

    private void answer(String callbackId, String text) {
        try {
            bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).text(text).build());
        } catch (TelegramApiException e) {
            log.error("answer error: {}", e.getMessage());
        }
    }

    private void send(Long chatId, String text) {
        try {
            bot.execute(SendMessage.builder().chatId(chatId.toString()).text(text).build());
        } catch (TelegramApiException e) {
            log.error("send error: {}", e.getMessage());
        }
    }

    private void send(Long chatId, String text, ReplyKeyboardMarkup keyboard) {
        try {
            bot.execute(SendMessage.builder().chatId(chatId.toString())
                    .text(text).replyMarkup(keyboard).build());
        } catch (TelegramApiException e) {
            log.error("send error: {}", e.getMessage());
        }
    }

    private void sendInline(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            bot.execute(SendMessage.builder().chatId(chatId.toString())
                    .text(text).replyMarkup(keyboard).build());
        } catch (TelegramApiException e) {
            log.error("sendInline error: {}", e.getMessage());
        }
    }

    private void editText(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        try {
            bot.execute(EditMessageText.builder()
                    .chatId(chatId.toString()).messageId(messageId)
                    .text(text).parseMode("HTML").replyMarkup(keyboard).build());
        } catch (TelegramApiException e) {
            log.error("editText error: {}", e.getMessage());
        }
    }

    private void editButtons(Long chatId, Integer messageId, InlineKeyboardMarkup keyboard) {
        try {
            bot.execute(EditMessageReplyMarkup.builder()
                    .chatId(chatId.toString()).messageId(messageId).replyMarkup(keyboard).build());
        } catch (TelegramApiException e) {
            log.error("editButtons error: {}", e.getMessage());
        }
    }
}