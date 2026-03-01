package kg.rental.bot.handler;

import kg.rental.bot.Keyboards;
import kg.rental.bot.RentalBot;
import kg.rental.entity.Listing;
import kg.rental.entity.User;
import kg.rental.enums.*;
import kg.rental.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageHandler {

    private final RentalBot bot;
    private final UserService userService;
    private final ListingService listingService;
    private final TelegramChannelService channelService;
    private final NotificationService notificationService;
    private final MatchingService matchingService;
    private final Keyboards keyboards;
    private final ComplaintService complaintService;

    public void handle(Message msg) {
        long telegramId = msg.getFrom().getId();
        String text = msg.getText().trim();
        User user = userService.getOrCreate(msg.getFrom());

        if (user.isBanned()) { send(telegramId, "🚫 Ваш аккаунт заблокирован."); return; }
        if (text.equals("/start") || text.equals("◀️ Главное меню") || text.equals("/menu")) {
            handleStart(user);
            return;
        }
        UserState state = user.getState();
        switch (state) {
            case IDLE -> handleIdle(user, text);
            // Сдаю квартиру
            case RENT_OUT_DISTRICT    -> handleRentOutDistrict(user, text);
            case RENT_OUT_ROOMS       -> handleRentOutRooms(user, text);
            case RENT_OUT_PRICE       -> handleRentOutPrice(user, text);
            case RENT_OUT_FURNITURE   -> handleRentOutFurniture(user, text);
            case RENT_OUT_UTILITIES   -> handleRentOutUtilities(user, text);
            case RENT_OUT_TENANT_TYPE -> send(telegramId, "Выберите варианты из кнопок выше и нажмите ✅ Готово 👆");
            case RENT_OUT_CONTACT     -> handleRentOutContact(user, text);
            case RENT_OUT_PHOTOS      -> handleRentOutPhotos(user, text);
            case RENT_OUT_DESCRIPTION -> handleRentOutDescription(user, text);
            // Ищу квартиру
            case RENT_IN_DISTRICT    -> handleRentInDistrict(user, text);
            case RENT_IN_BUDGET      -> handleRentInBudget(user, text);
            case RENT_IN_ROOMS       -> handleRentInRooms(user, text);
            case RENT_IN_WHEN        -> handleRentInWhen(user, text);
            case RENT_IN_CONTACT     -> handleRentInContact(user, text);
            case RENT_IN_DESCRIPTION -> handleRentInDescription(user, text);
            // Сниму комнату
            case RENT_ROOM_IN_DISTRICT    -> handleRentRoomInDistrict(user, text);
            case RENT_ROOM_IN_WHO         -> handleRentRoomInWho(user, text);
            case RENT_ROOM_IN_BUDGET      -> handleRentRoomInBudget(user, text);
            case RENT_ROOM_IN_WHEN        -> handleRentRoomInWhen(user, text);
            case RENT_ROOM_IN_CONTACT     -> handleRentRoomInContact(user, text);
            case RENT_ROOM_IN_DESCRIPTION -> handleRentRoomInDescription(user, text);
            // Ищу подселение
            case ROOMMATE_SEEK_DISTRICT    -> handleRoommateSeekDistrict(user, text);
            case ROOMMATE_SEEK_BUDGET      -> handleRoommateSeekBudget(user, text);
            case ROOMMATE_SEEK_GENDER      -> handleRoommateSeekGender(user, text);
            case ROOMMATE_SEEK_SPOTS       -> send(telegramId, "Выберите количество из кнопок выше 👆");
            case ROOMMATE_SEEK_WHEN        -> handleRoommateSeekWhen(user, text);
            case ROOMMATE_SEEK_CONTACT     -> handleRoommateSeekContact(user, text);
            case ROOMMATE_SEEK_DESCRIPTION -> handleRoommateSeekDescription(user, text);
            // Сдаю место
            case ROOMMATE_OFFER_TYPE        -> send(telegramId, "Выберите вариант из кнопок выше 👆");
            case ROOMMATE_OFFER_DISTRICT    -> handleRoommateOfferDistrict(user, text);
            case ROOMMATE_OFFER_PRICE       -> handleRoommateOfferPrice(user, text);
            case ROOMMATE_OFFER_SPOTS       -> handleRoommateOfferSpots(user, text);
            case ROOMMATE_OFFER_GENDER      -> send(telegramId, "Выберите варианты из кнопок выше и нажмите ✅ Готово 👆");
            case ROOMMATE_OFFER_AMENITIES   -> handleRoommateOfferAmenities(user, text);
            case ROOMMATE_OFFER_CONTACT     -> handleRoommateOfferContact(user, text);
            case ROOMMATE_OFFER_PHOTOS      -> handleRoommateOfferPhotos(user, text);
            case ROOMMATE_OFFER_DESCRIPTION -> handleRoommateOfferDescription(user, text);
            default -> handleStart(user);
        }
    }

    public void handlePhoto(Message msg) {
        long telegramId = msg.getFrom().getId();
        UserState state = userService.getState(telegramId);
        User user = userService.getOrCreate(msg.getFrom());

        if (state == UserState.RENT_OUT_PHOTOS || state == UserState.ROOMMATE_OFFER_PHOTOS) {
            String fileId = msg.getPhoto().get(msg.getPhoto().size() - 1).getFileId();
            Map<String, Object> draft = userService.getDraft(user.getId());
            @SuppressWarnings("unchecked")
            List<String> photos = (List<String>) draft.getOrDefault("photos", new ArrayList<>());
            photos.add(fileId);
            userService.saveDraftField(user.getId(), "photos", photos);
            if (photos.size() >= 3) {
                send(telegramId, "✅ " + photos.size() + " фото. Ещё или 'Готово ✅'", keyboards.skipOrFinish());
            } else {
                send(telegramId, "📷 Фото " + photos.size() + "/3. Ещё или 'Пропустить ⏭'", keyboards.skipOrFinish());
            }
        }
    }

    // ── Start / Idle ──

    private void handleStart(User user) {
        userService.setState(user.getTelegramId(), UserState.IDLE);
        userService.clearDraft(user.getId());
        send(user.getTelegramId(),
                "Привет, " + user.getFirstName() + "! 👋\n\n" +
                        "🏠 Добро пожаловать в Аренда Бишкек\n\n" +
                        "Выберите действие:", keyboards.mainMenu());
    }

    private void handleIdle(User user, String text) {
        switch (text) {
            case "🏠 Сдать квартиру" -> startRentOut(user);
            case "🔍 Ищу квартиру"   -> startRentIn(user);
            case "🛏 Сниму комнату"  -> startRentRoomIn(user);
            case "🛋 Сдаю место"     -> startRoommateOffer(user);
            case "👥 Ищу подселение" -> startRoommateSeek(user);
            case "📋 Мои объявления" -> showMyListings(user);
            default -> send(user.getTelegramId(), "Выберите действие 👇", keyboards.mainMenu());
        }
    }

    // ── Сдаю квартиру ──

    private void startRentOut(User user) {
        userService.saveDraftField(user.getId(), "type", ListingType.RENT_OUT.name());
        userService.setState(user.getTelegramId(), UserState.RENT_OUT_DISTRICT);
        send(user.getTelegramId(), "📍 В каком районе квартира? (напишите, например: Центр, Джал, Асанбай)");
    }

    private void handleRentOutDistrict(User user, String text) {
        userService.saveDraftField(user.getId(), "district", text);
        userService.setState(user.getTelegramId(), UserState.RENT_OUT_ROOMS);
        send(user.getTelegramId(), "🏠 Сколько комнат?", keyboards.rooms());
    }

    private void handleRentOutRooms(User user, String text) {
        try {
            userService.saveDraftField(user.getId(), "rooms", text.equals("5+") ? 5 : Integer.parseInt(text));
            userService.setState(user.getTelegramId(), UserState.RENT_OUT_PRICE);
            send(user.getTelegramId(), "💰 Цена в месяц (сом)?");
        } catch (NumberFormatException e) { send(user.getTelegramId(), "Выберите 👇", keyboards.rooms()); }
    }

    private void handleRentOutPrice(User user, String text) {
        try {
            userService.saveDraftField(user.getId(), "price", Integer.parseInt(text.replaceAll("[^0-9]", "")));
            userService.setState(user.getTelegramId(), UserState.RENT_OUT_FURNITURE);
            send(user.getTelegramId(), "🪑 Что есть в квартире?", keyboards.furniture());
        } catch (NumberFormatException e) { send(user.getTelegramId(), "Введите цену числом, например: 15000"); }
    }

    private void handleRentOutFurniture(User user, String text) {
        userService.saveDraftField(user.getId(), "furniture", text.contains("Мебель"));
        userService.saveDraftField(user.getId(), "appliances", text.contains("Техника"));
        userService.setState(user.getTelegramId(), UserState.RENT_OUT_UTILITIES);
        send(user.getTelegramId(), "💡 Коммуналка включена?", keyboards.yesNo());
    }

    private void handleRentOutUtilities(User user, String text) {
        userService.saveDraftField(user.getId(), "utilitiesIncluded", text.contains("Да") || text.contains("✅"));
        userService.setState(user.getTelegramId(), UserState.RENT_OUT_TENANT_TYPE);
        userService.saveDraftField(user.getId(), "tenantTypes", new ArrayList<>());
        sendInline(user.getTelegramId(), "👤 Кому сдаёте? (можно выбрать несколько)",
                keyboards.tenantTypeMultiInline(new LinkedHashSet<>()));
    }

    private void handleRentOutContact(User user, String text) {
        userService.saveDraftField(user.getId(), "contact", text);
        userService.setState(user.getTelegramId(), UserState.RENT_OUT_PHOTOS);
        send(user.getTelegramId(), "📷 Отправьте фото (минимум 3) или 'Пропустить ⏭'", keyboards.skipOrFinish());
    }

    private void handleRentOutPhotos(User user, String text) {
        if (text.equals("Пропустить ⏭") || text.equals("Готово ✅")) {
            userService.setState(user.getTelegramId(), UserState.RENT_OUT_DESCRIPTION);
            send(user.getTelegramId(), "📝 Описание (необязательно)", keyboards.skipOrFinish());
        } else { send(user.getTelegramId(), "📷 Отправьте фото или 'Пропустить ⏭'", keyboards.skipOrFinish()); }
    }

    private void handleRentOutDescription(User user, String text) {
        if (!text.equals("Пропустить ⏭") && !text.equals("Готово ✅"))
            userService.saveDraftField(user.getId(), "description", text);
        publishAndFinish(user);
    }

    // ── Ищу квартиру ──

    private void startRentIn(User user) {
        userService.saveDraftField(user.getId(), "type", ListingType.RENT_IN.name());
        userService.setState(user.getTelegramId(), UserState.RENT_IN_DISTRICT);
        send(user.getTelegramId(), "📍 В каком районе ищете? (напишите, например: Центр, Джал, Асанбай)");
    }

    private void handleRentInDistrict(User user, String text) {
        userService.saveDraftField(user.getId(), "district", text);
        userService.setState(user.getTelegramId(), UserState.RENT_IN_BUDGET);
        send(user.getTelegramId(), "💰 Ваш бюджет?", keyboards.budgetRangesApartment());
    }

    private void handleRentInBudget(User user, String text) {
        userService.saveDraftField(user.getId(), "priceRange", text);
        userService.setState(user.getTelegramId(), UserState.RENT_IN_ROOMS);
        send(user.getTelegramId(), "🏠 Сколько комнат?", keyboards.rooms());
    }

    private void handleRentInRooms(User user, String text) {
        try {
            userService.saveDraftField(user.getId(), "rooms", text.equals("5+") ? 5 : Integer.parseInt(text));
            userService.setState(user.getTelegramId(), UserState.RENT_IN_WHEN);
            send(user.getTelegramId(), "📅 Когда нужно?", keyboards.when());
        } catch (NumberFormatException e) { send(user.getTelegramId(), "Выберите 👇", keyboards.rooms()); }
    }

    private void handleRentInWhen(User user, String text) {
        userService.saveDraftField(user.getId(), "when", text);
        userService.setState(user.getTelegramId(), UserState.RENT_IN_CONTACT);
        send(user.getTelegramId(), "📞 Ваш контакт для связи (номер или @username)?");
    }

    private void handleRentInContact(User user, String text) {
        userService.saveDraftField(user.getId(), "contact", text);
        userService.setState(user.getTelegramId(), UserState.RENT_IN_DESCRIPTION);
        send(user.getTelegramId(), "📝 Описание (необязательно)", keyboards.skipOrFinish());
    }

    private void handleRentInDescription(User user, String text) {
        if (!text.equals("Пропустить ⏭") && !text.equals("Готово ✅"))
            userService.saveDraftField(user.getId(), "description", text);
        publishAndFinish(user);
    }

    // ── Сниму комнату ──

    private void startRentRoomIn(User user) {
        userService.saveDraftField(user.getId(), "type", ListingType.RENT_ROOM_IN.name());
        userService.setState(user.getTelegramId(), UserState.RENT_ROOM_IN_DISTRICT);
        send(user.getTelegramId(), "📍 В каком районе ищете комнату? (напишите, например: Центр, Джал, Асанбай)");
    }

    private void handleRentRoomInDistrict(User user, String text) {
        userService.saveDraftField(user.getId(), "district", text);
        userService.setState(user.getTelegramId(), UserState.RENT_ROOM_IN_WHO);
        send(user.getTelegramId(), "👤 Кто вы?", keyboards.whoAreYouFull());
    }

    private void handleRentRoomInWho(User user, String text) {
        userService.saveDraftField(user.getId(), "myGender", text);
        userService.setState(user.getTelegramId(), UserState.RENT_ROOM_IN_BUDGET);
        send(user.getTelegramId(), "💰 Ваш бюджет за комнату?", keyboards.budgetRangesRoom());
    }

    private void handleRentRoomInBudget(User user, String text) {
        userService.saveDraftField(user.getId(), "priceRange", text);
        userService.setState(user.getTelegramId(), UserState.RENT_ROOM_IN_WHEN);
        send(user.getTelegramId(), "📅 Когда нужно?", keyboards.when());
    }

    private void handleRentRoomInWhen(User user, String text) {
        userService.saveDraftField(user.getId(), "when", text);
        userService.setState(user.getTelegramId(), UserState.RENT_ROOM_IN_CONTACT);
        send(user.getTelegramId(), "📞 Ваш контакт для связи (номер или @username)?");
    }

    private void handleRentRoomInContact(User user, String text) {
        userService.saveDraftField(user.getId(), "contact", text);
        userService.setState(user.getTelegramId(), UserState.RENT_ROOM_IN_DESCRIPTION);
        send(user.getTelegramId(), "📝 Описание (необязательно, например: нужна мебель, с балконом и т.д.)",
                keyboards.skipOrFinish());
    }

    private void handleRentRoomInDescription(User user, String text) {
        if (!text.equals("Пропустить ⏭") && !text.equals("Готово ✅"))
            userService.saveDraftField(user.getId(), "description", text);
        publishAndFinish(user);
    }

    // ── Ищу подселение ──

    private void startRoommateSeek(User user) {
        userService.saveDraftField(user.getId(), "type", ListingType.ROOMMATE_SEEK.name());
        userService.setState(user.getTelegramId(), UserState.ROOMMATE_SEEK_DISTRICT);
        send(user.getTelegramId(), "📍 В каком районе ищете? (напишите, например: Центр, Джал, Асанбай)");
    }

    private void handleRoommateSeekDistrict(User user, String text) {
        userService.saveDraftField(user.getId(), "district", text);
        userService.setState(user.getTelegramId(), UserState.ROOMMATE_SEEK_BUDGET);
        send(user.getTelegramId(), "💰 Ваш бюджет за место?", keyboards.budgetRangesRoommate());
    }

    private void handleRoommateSeekBudget(User user, String text) {
        userService.saveDraftField(user.getId(), "priceRange", text);
        userService.setState(user.getTelegramId(), UserState.ROOMMATE_SEEK_GENDER);
        send(user.getTelegramId(), "👤 Кто вы?", keyboards.whoAreYou());
    }

    private void handleRoommateSeekGender(User user, String text) {
        userService.saveDraftField(user.getId(), "myGender",
                text.contains("Девушка") ? Gender.FEMALE.name() : Gender.MALE.name());
        userService.setState(user.getTelegramId(), UserState.ROOMMATE_SEEK_SPOTS);
        sendInline(user.getTelegramId(), "🔢 Сколько мест ищете?", keyboards.seekSpotsInline());
    }

    private void handleRoommateSeekWhen(User user, String text) {
        userService.saveDraftField(user.getId(), "when", text);
        userService.setState(user.getTelegramId(), UserState.ROOMMATE_SEEK_CONTACT);
        send(user.getTelegramId(), "📞 Ваш контакт для связи (номер или @username)?");
    }

    private void handleRoommateSeekContact(User user, String text) {
        userService.saveDraftField(user.getId(), "contact", text);
        userService.setState(user.getTelegramId(), UserState.ROOMMATE_SEEK_DESCRIPTION);
        send(user.getTelegramId(), "📝 Описание (необязательно)", keyboards.skipOrFinish());
    }

    private void handleRoommateSeekDescription(User user, String text) {
        if (!text.equals("Пропустить ⏭") && !text.equals("Готово ✅"))
            userService.saveDraftField(user.getId(), "description", text);
        publishAndFinish(user);
    }

    // ── Сдаю место ──

    private void startRoommateOffer(User user) {
        userService.saveDraftField(user.getId(), "type", ListingType.ROOMMATE_OFFER.name());
        userService.setState(user.getTelegramId(), UserState.ROOMMATE_OFFER_TYPE);
        sendInline(user.getTelegramId(), "🏠 Что вы сдаёте?", keyboards.offerRoomTypeInline());
    }

    private void handleRoommateOfferDistrict(User user, String text) {
        userService.saveDraftField(user.getId(), "district", text);
        userService.setState(user.getTelegramId(), UserState.ROOMMATE_OFFER_PRICE);
        send(user.getTelegramId(), "💰 Цена за одно место (сом/мес)?");
    }

    private void handleRoommateOfferPrice(User user, String text) {
        try {
            userService.saveDraftField(user.getId(), "price", Integer.parseInt(text.replaceAll("[^0-9]", "")));
            userService.setState(user.getTelegramId(), UserState.ROOMMATE_OFFER_SPOTS);
            send(user.getTelegramId(), "🛏 Сколько мест свободно?", keyboards.rooms());
        } catch (NumberFormatException e) { send(user.getTelegramId(), "Введите цену числом"); }
    }

    private void handleRoommateOfferSpots(User user, String text) {
        try {
            userService.saveDraftField(user.getId(), "spotsAvailable", Integer.parseInt(text.replaceAll("[^0-9]", "")));
            userService.setState(user.getTelegramId(), UserState.ROOMMATE_OFFER_GENDER);
            userService.saveDraftField(user.getId(), "tenantTypes", new ArrayList<>());
            sendInline(user.getTelegramId(), "👥 Кого берёте? (можно выбрать несколько)",
                    keyboards.tenantTypeMultiInline(new LinkedHashSet<>()));
        } catch (NumberFormatException e) { send(user.getTelegramId(), "Введите количество"); }
    }

    private void handleRoommateOfferAmenities(User user, String text) {
        if (!text.equals("Пропустить ⏭"))
            userService.saveDraftField(user.getId(), "amenities", text);
        userService.setState(user.getTelegramId(), UserState.ROOMMATE_OFFER_CONTACT);
        send(user.getTelegramId(), "📞 Ваш контакт для связи (номер или @username)?");
    }

    private void handleRoommateOfferContact(User user, String text) {
        userService.saveDraftField(user.getId(), "contact", text);
        userService.setState(user.getTelegramId(), UserState.ROOMMATE_OFFER_PHOTOS);
        send(user.getTelegramId(), "📷 Фото комнаты или 'Пропустить ⏭'", keyboards.skipOrFinish());
    }

    private void handleRoommateOfferPhotos(User user, String text) {
        if (text.equals("Пропустить ⏭") || text.equals("Готово ✅")) {
            userService.setState(user.getTelegramId(), UserState.ROOMMATE_OFFER_DESCRIPTION);
            send(user.getTelegramId(), "📝 Описание (необязательно)", keyboards.skipOrFinish());
        } else { send(user.getTelegramId(), "📷 Отправьте фото или 'Пропустить ⏭'", keyboards.skipOrFinish()); }
    }

    private void handleRoommateOfferDescription(User user, String text) {
        if (!text.equals("Пропустить ⏭") && !text.equals("Готово ✅"))
            userService.saveDraftField(user.getId(), "description", text);
        publishAndFinish(user);
    }

    // ── Публикация ──

    private void publishAndFinish(User user) {
        Map<String, Object> draft = userService.getDraft(user.getId());
        Listing listing = listingService.createFromDraft(user.getTelegramId(), draft);
        int msgId = channelService.publishListing(listing);
        listingService.saveChannelMessageId(listing.getId(), msgId);

        List<Long> subscribers = notificationService.findSubscribers(listing);
        subscribers.forEach(tgId -> channelService.sendNotification(tgId, listing));

        List<Long> matches = matchingService.findMatches(listing);
        matches.forEach(tgId -> channelService.sendMatchNotification(tgId, listing));

        userService.clearDraft(user.getId());
        userService.setState(user.getTelegramId(), UserState.IDLE);
        send(user.getTelegramId(),
                "✅ Объявление опубликовано!\n\n" +
                        "ID: #" + listing.getId() + "\n" +
                        "Через 3 дня спрошу, актуально ли оно.\n\n" +
                        "Что ещё?", keyboards.mainMenu());
    }

    // ── Мои объявления ──

    private void showMyListings(User user) {
        List<Listing> listings = listingService.findActiveByUser(user.getTelegramId());
        if (listings.isEmpty()) {
            send(user.getTelegramId(), "📋 Нет активных объявлений.\n\nСоздайте первое!", keyboards.mainMenu());
            return;
        }
        send(user.getTelegramId(), "📋 <b>Ваши объявления (" + listings.size() + "):</b>",
                keyboards.backToMenu());
        for (Listing l : listings) {
            StringBuilder sb = new StringBuilder();
            sb.append(l.getStatus().getEmoji()).append(" <b>#").append(l.getId()).append("</b>\n");
            sb.append("📍 ").append(l.getDistrict()).append("\n");
            if (l.getPrice() != null) sb.append("💰 ").append(l.getPrice()).append(" сом/мес\n");
            if (l.getPriceRange() != null) sb.append("💰 ").append(l.getPriceRange()).append("\n");
            sb.append("📞 ").append(l.getContact());
            if (l.getExpiresAt() != null)
                sb.append("\n⏳ До: ").append(l.getExpiresAt().toLocalDate());
            sendInline(user.getTelegramId(), sb.toString(),
                    keyboards.myListingActions(l.getId(), l.getStatus()));
        }
    }

    // ── helpers ──

    private void send(Long chatId, String text) {
        send(chatId, text, (ReplyKeyboardMarkup) null);
    }

    private void send(Long chatId, String text, ReplyKeyboardMarkup keyboard) {
        try {
            SendMessage.SendMessageBuilder builder = SendMessage.builder()
                    .chatId(chatId.toString()).text(text).parseMode("HTML");
            if (keyboard != null) builder.replyMarkup(keyboard);
            bot.execute(builder.build());
        } catch (TelegramApiException e) {
            log.error("send error to {}: {}", chatId, e.getMessage());
        }
    }

    private void sendInline(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            bot.execute(SendMessage.builder()
                    .chatId(chatId.toString()).text(text).parseMode("HTML").replyMarkup(keyboard).build());
        } catch (TelegramApiException e) {
            log.error("sendInline error to {}: {}", chatId, e.getMessage());
        }
    }
}