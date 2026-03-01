package kg.rental.service;

import kg.rental.entity.Listing;
import kg.rental.enums.Gender;
import kg.rental.enums.ListingType;
import org.springframework.stereotype.Component;

@Component
public class MessageHelper {

    private static final String BOT_LINK = "@Bishkek_RentKg_bot";

    public String formatListing(Listing l) {
        StringBuilder sb = new StringBuilder();

        sb.append(l.getStatus().getEmoji()).append(" <b>");
        sb.append(switch (l.getType()) {
            case RENT_OUT      -> "СДАЁТСЯ КВАРТИРА";
            case RENT_IN       -> "ИЩУТ КВАРТИРУ";
            case RENT_ROOM_IN  -> "СНИМАЮТ КОМНАТУ";
            case ROOMMATE_SEEK  -> "ИЩУ ПОДСЕЛЕНИЕ";
            case ROOMMATE_OFFER -> "СДАЁТСЯ МЕСТО";
            case COMMERCIAL_RENT_OUT -> "СДАЁТСЯ ПОМЕЩЕНИЕ";
        });
        sb.append("</b>\n");
        sb.append("──────────────────\n");
        sb.append("📍 <b>Район:</b> ").append(l.getDistrict()).append("\n");

        if (l.getRooms() != null)
            sb.append("🏠 <b>Комнат:</b> ").append(l.getRooms()).append("\n");

        if (l.getPriceRange() != null && !l.getPriceRange().isBlank())
            sb.append("💰 <b>Бюджет:</b> ").append(l.getPriceRange()).append(" сом/мес\n");
        else if (l.getPrice() != null)
            sb.append("💰 <b>Цена:</b> ").append(formatPrice(l.getPrice())).append(" сом/мес\n");

        // ── RENT_OUT ──
        if (l.getType() == ListingType.RENT_OUT) {
            sb.append("🪑 <b>Мебель:</b> ").append(l.isFurniture() ? "✅ есть" : "❌ нет").append("\n");
            sb.append("📱 <b>Техника:</b> ").append(l.isAppliances() ? "✅ есть" : "❌ нет").append("\n");
            sb.append("💡 <b>Коммуналка:</b> ").append(l.isUtilitiesIncluded() ? "включена" : "не включена").append("\n");
            if (l.getTenantType() != null && !l.getTenantType().isBlank())
                sb.append("👤 <b>Для кого:</b> ").append(formatTenantType(l.getTenantType())).append("\n");
        }
        // ── RENT_IN ──
        if (l.getType() == ListingType.RENT_IN) {
            if (l.getMyGender() != null)
                sb.append("👤 <b>Кто снимает:</b> ").append(genderLabel(l.getMyGender())).append("\n");
        }

        // ── RENT_ROOM_IN ──
        if (l.getType() == ListingType.RENT_ROOM_IN) {
            if (l.getMyGender() != null)
                sb.append("👤 <b>Я:</b> ").append(genderLabel(l.getMyGender())).append("\n");
        }

        // ── ROOMMATE_OFFER ──
        if (l.getType() == ListingType.ROOMMATE_OFFER) {
            if (l.getSpotsAvailable() != null)
                sb.append("🛏 <b>Мест:</b> ").append(l.getSpotsAvailable()).append("\n");
            if (l.getOfferRoomType() != null)
                sb.append("🚪 <b>Тип:</b> ")
                        .append("ROOM".equals(l.getOfferRoomType()) ? "Комната целиком" : "Место в комнате")
                        .append("\n");
            if (l.getTenantType() != null && !l.getTenantType().isBlank())
                sb.append("👥 <b>Берём:</b> ").append(formatTenantType1(l.getTenantType())).append("\n");
        }
// ── COMMERCIAL_RENT_OUT ──
        if (l.getType() == ListingType.COMMERCIAL_RENT_OUT) {
            sb.append("💡 <b>Коммуналка:</b> ").append(l.isUtilitiesIncluded() ? "включена" : "не включена").append("\n");
            if (l.getDescription() != null && !l.getDescription().isBlank())
                sb.append("📝 ").append(l.getDescription()).append("\n");
        }
        // ── ROOMMATE_SEEK ──
        if (l.getType() == ListingType.ROOMMATE_SEEK) {
            if (l.getMyGender() != null)
                sb.append("👤 <b>Я:</b> ").append(genderLabel(l.getMyGender())).append("\n");
            if (l.getSpotsAvailable() != null)
                sb.append("🔢 <b>Ищу мест:</b> ").append(l.getSpotsAvailable()).append("\n");
        }

        if (l.getDescription() != null && !l.getDescription().isBlank())
            sb.append("\n📝 ").append(l.getDescription()).append("\n");

        sb.append("──────────────────\n");
        sb.append("📞 <b>Контакт:</b> ").append(l.getContact()).append("\n\n");
        sb.append("#").append(l.getDistrict().replace(" ", "_")).append(" ");
        sb.append("#").append(l.getType().name().toLowerCase()).append("\n\n");
        sb.append("📌 <i>Подать объявление: </i>").append(BOT_LINK);

        return sb.toString();
    }

    public String formatBlacklistWarning(Listing l) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚫 <b>ОБЪЯВЛЕНИЕ ЗАБЛОКИРОВАНО</b>\n");
        sb.append("──────────────────\n");

        // Инфо о пользователе
        sb.append("👤 <b>Пользователь:</b>\n");
        sb.append("   • ID: <code>").append(l.getUser().getTelegramId()).append("</code>\n");
        if (l.getUser().getUsername() != null)
            sb.append("   • Username: @").append(l.getUser().getUsername()).append("\n");
        sb.append("   • Имя: ").append(l.getUser().getFirstName()).append("\n");
        if (l.getUser().getPhone() != null)
            sb.append("   • Телефон: ").append(l.getUser().getPhone()).append("\n");

        sb.append("──────────────────\n");

        // Инфо об объявлении
        sb.append("📋 <b>Объявление #").append(l.getId()).append("</b>\n");
        sb.append("📍 Район: ").append(l.getDistrict()).append("\n");
        sb.append("🏷 Тип: ").append(switch (l.getType()) {
            case RENT_OUT      -> "Сдаётся квартира";
            case RENT_IN       -> "Ищут квартиру";
            case RENT_ROOM_IN  -> "Снимают комнату";
            case ROOMMATE_SEEK  -> "Ищу подселение";
            case ROOMMATE_OFFER -> "Сдаётся место";
            case COMMERCIAL_RENT_OUT -> "Сдаётся помещение";
        }).append("\n");

        if (l.getPrice() != null)
            sb.append("💰 Цена: ").append(l.getPrice()).append(" сом/мес\n");
        if (l.getPriceRange() != null)
            sb.append("💰 Бюджет: ").append(l.getPriceRange()).append("\n");
        if (l.getRooms() != null)
            sb.append("🏠 Комнат: ").append(l.getRooms()).append("\n");

        sb.append("📞 Контакт: ").append(l.getContact()).append("\n");

        if (l.getDescription() != null && !l.getDescription().isBlank())
            sb.append("📝 Описание: ").append(l.getDescription()).append("\n");

        sb.append("──────────────────\n");
        sb.append("⚠️ Получена жалоба на мошенничество.\n");
        sb.append("🔒 Пользователь ограничен на 7 дней.");

        return sb.toString();
    }

    public String formatReminderMessage(Long listingId) {
        return "⏰ Ваше объявление <b>#" + listingId + "</b> опубликовано 3 дня назад.\n\n" +
                "Квартира/место ещё актуально?";
    }

    public String formatNotification(Listing l) {
        return "🔔 <b>Новое объявление в вашем районе</b> (" + l.getDistrict() + ")\n\n" +
                formatListing(l);
    }

    // ── helpers ──

    private String formatTenantType(String tenantTypeStr) {
        if (tenantTypeStr == null || tenantTypeStr.isBlank()) return "Всем";
        String[] parts = tenantTypeStr.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(tenantTypePartLabel(parts[i].trim()));
        }
        return sb.toString();
    }
    private String formatTenantType1(String tenantTypeStr) {
        if (tenantTypeStr == null || tenantTypeStr.isBlank()) return "Всем";
        String[] parts = tenantTypeStr.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(tenantTypePartLabel1(parts[i].trim()));
        }
        return sb.toString();
    }

    private String tenantTypePartLabel(String value) {
        return switch (value) {
            case "ANY"    -> "👨‍👩‍👧 Всем";
            case "FAMILY" -> "👪 Семьям";
            case "FEMALE" -> "👩 Девушкам";
            case "MALE"   -> "👨 Парням";
            default       -> value;
        };
    }
    private String tenantTypePartLabel1(String value) {
        return switch (value) {
            case "ANY"    -> "👨‍👩‍👧 Всех";
            case "FAMILY" -> "👪 Семью";
            case "FEMALE" -> "👩 Девушек";
            case "MALE"   -> "👨 Парней";
            default       -> value;
        };
    }

    private String genderLabel(Gender gender) {
        if (gender == null) return "";
        return switch (gender) {
            case FEMALE -> "👩 Девушка";
            case MALE   -> "👨 Парень";
            default     -> "🤷 Не указано";
        };
    }

    private String formatPrice(int price) {
        return String.format("%,d", price).replace(",", " ");
    }
}