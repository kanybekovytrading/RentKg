package kg.rental.dto;

import kg.rental.enums.ComplaintReason;
import lombok.Data;

@Data
public class ComplaintRequest {
    private Long listingId;
    private Long reporterTelegramId;
    private ComplaintReason reason;
    private String description;
}
