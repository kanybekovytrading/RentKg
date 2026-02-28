package kg.rental.enums;

public enum Gender {
    MALE, FEMALE, FAMILY, MIXED, ANY;

    public String getLabel() {
        return switch (this) {
            case MALE   -> "Только парней";
            case FEMALE -> "Только девушек";
            case FAMILY -> "Только семьям";
            case MIXED  -> "Смешанно";
            case ANY    -> "Неважно";
        };
    }
}
