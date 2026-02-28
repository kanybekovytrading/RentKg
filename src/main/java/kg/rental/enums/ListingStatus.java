package kg.rental.enums;

public enum ListingStatus {
    ACTIVE,    // 🟢
    PENDING,   // 🟡
    EXPIRED,   // 🔴
    CLOSED,
    ARCHIVED;

    public String getEmoji() {
        return switch (this) {
            case ACTIVE  -> "🟢";
            case PENDING -> "🟡";
            case EXPIRED -> "🔴";
            case CLOSED  -> "✅";
            case ARCHIVED -> "📦";
        };
    }
}
