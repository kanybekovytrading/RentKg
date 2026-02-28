package kg.rental.enums;

public enum ComplaintReason {
    SCAMMER, PHOTO_MISMATCH, ALREADY_RENTED, OTHER;

    public String getLabel() {
        return switch (this) {
            case SCAMMER        -> "Мошенник";
            case PHOTO_MISMATCH -> "Фото не совпадает";
            case ALREADY_RENTED -> "Уже сдана";
            case OTHER          -> "Другое";
        };
    }
}
