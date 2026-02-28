package kg.rental.dto;

import kg.rental.enums.Gender;
import kg.rental.enums.ListingStatus;
import kg.rental.enums.ListingType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ListingDto {
    private Long id;
    private ListingType type;
    private ListingStatus status;
    private String statusEmoji;
    private String district;
    private Integer rooms;
    private Integer price;
    private boolean utilitiesIncluded;
    private boolean furniture;
    private boolean appliances;
    private Gender tenantType;
    private Integer spotsAvailable;
    private Gender preferredGender;
    private String contact;
    private String description;
    private String[] photoFileIds;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Long userId;
}
