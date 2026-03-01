package kg.rental.bot;

import kg.rental.enums.ListingStatus;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class Keyboards {

    // ── Главное меню ──

    public ReplyKeyboardMarkup mainMenu() {
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(
                        new KeyboardRow(List.of(
                                new KeyboardButton("🏠 Сдать квартиру"),
                                new KeyboardButton("🔍 Ищу квартиру")
                        )),
                        new KeyboardRow(List.of(
                                new KeyboardButton("🛏 Сниму комнату"),
                                new KeyboardButton("🛋 Сдаю место")
                        )),
                        new KeyboardRow(List.of(
                                new KeyboardButton("👥 Ищу подселение"),
                                new KeyboardButton("📋 Мои объявления")
                        ))
                ))
                .resizeKeyboard(true)
                .isPersistent(true)
                .build();
    }

    public ReplyKeyboardMarkup yesNo() {
        return replyKeyboard(List.of(List.of("✅ Да", "❌ Нет")));
    }

    public ReplyKeyboardMarkup rooms() {
        return replyKeyboard(List.of(
                List.of("1", "2", "3"),
                List.of("4", "5+")
        ));
    }

    // Для "Ищу подселение" — только Девушка / Парень
    public ReplyKeyboardMarkup whoAreYou() {
        return replyKeyboard(List.of(List.of("👩 Девушка", "👨 Парень")));
    }

    // Для "Сниму комнату" — Девушка / Парень / Семья
    public ReplyKeyboardMarkup whoAreYouFull() {
        return replyKeyboard(List.of(
                List.of("👩 Девушка", "👨 Парень", "👪 Семья")
        ));
    }

    public ReplyKeyboardMarkup when() {
        return replyKeyboard(List.of(
                List.of("🔥 Срочно", "📅 В течение недели"),
                List.of("🗓 В этом месяце")
        ));
    }

    public ReplyKeyboardMarkup skipOrFinish() {
        return replyKeyboard(List.of(List.of("Пропустить ⏭", "Готово ✅")));
    }

    public ReplyKeyboardMarkup furniture() {
        return replyKeyboard(List.of(
                List.of("🪑 Мебель есть", "📱 Техника есть"),
                List.of("🪑📱 Мебель + техника", "❌ Без мебели")
        ));
    }

    public ReplyKeyboardMarkup budgetRangesApartment() {
        return replyKeyboard(List.of(
                List.of("до 10 000", "10 000 – 20 000"),
                List.of("20 000 – 30 000", "от 30 000")
        ));
    }

    public ReplyKeyboardMarkup budgetRangesRoom() {
        return replyKeyboard(List.of(
                List.of("до 8 000", "8 000 – 12 000"),
                List.of("12 000 – 18 000", "от 18 000")
        ));
    }

    public ReplyKeyboardMarkup budgetRangesRoommate() {
        return replyKeyboard(List.of(
                List.of("до 5 000", "5 000 – 8 000"),
                List.of("8 000 – 12 000", "от 12 000")
        ));
    }

    public ReplyKeyboardMarkup backToMenu() {
        return replyKeyboard(List.of(List.of("◀️ Главное меню")));
    }

    // ── Мультиселект "для кого сдаёшь" ──

    public InlineKeyboardMarkup tenantTypeMultiInline(Set<String> selected) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(
                                checkBtn("👨‍👩‍👧 Всем", "tenant_toggle:ANY", selected.contains("ANY")),
                                checkBtn("👪 Семье", "tenant_toggle:FAMILY", selected.contains("FAMILY"))
                        ),
                        List.of(
                                checkBtn("👩 Девушкам", "tenant_toggle:FEMALE", selected.contains("FEMALE")),
                                checkBtn("👨 Парням", "tenant_toggle:MALE", selected.contains("MALE"))
                        ),
                        List.of(btn("✅ Готово", "tenant_done"))
                )).build();
    }

    public InlineKeyboardMarkup seekSpotsInline() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(btn("1", "seek_spots:1"), btn("2", "seek_spots:2"), btn("3+", "seek_spots:3"))
                )).build();
    }

    public InlineKeyboardMarkup offerRoomTypeInline() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(btn("🛏 Место в комнате", "offer_type:SPOT"), btn("🚪 Комнату целиком", "offer_type:ROOM"))
                )).build();
    }

    public InlineKeyboardMarkup complaintReasons(Long listingId) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(btn("🚨 Мошенник", "complaint_reason:" + listingId + ":SCAMMER"),
                                btn("📷 Фото не то", "complaint_reason:" + listingId + ":PHOTO_MISMATCH")),
                        List.of(btn("✅ Уже сдана", "complaint_reason:" + listingId + ":ALREADY_RENTED"),
                                btn("❓ Другое", "complaint_reason:" + listingId + ":OTHER"))
                )).build();
    }

    public InlineKeyboardMarkup myListingActions(Long listingId, ListingStatus status) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        if (status == ListingStatus.ACTIVE || status == ListingStatus.PENDING) {
            row.add(btn("❌ Неактуально", "my_close:" + listingId));
        } else {
            row.add(btn("🔄 Выложить ещё раз", "my_reopen:" + listingId));
        }
        return InlineKeyboardMarkup.builder().keyboardRow(row).build();
    }

    // ── helpers ──

    private ReplyKeyboardMarkup replyKeyboard(List<List<String>> rows) {
        List<KeyboardRow> keyboard = rows.stream().map(row -> {
            KeyboardRow r = new KeyboardRow();
            row.forEach(text -> r.add(new KeyboardButton(text)));
            return r;
        }).toList();
        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }

    private InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }

    private InlineKeyboardButton checkBtn(String text, String data, boolean selected) {
        String label = selected ? "✅ " + text : text;
        return InlineKeyboardButton.builder().text(label).callbackData(data).build();
    }
}