package kg.rental.dto;

import kg.rental.entity.Listing;
import kg.rental.enums.Gender;
import org.springframework.stereotype.Component;

@Component
public class ListingMapper {

    public ListingDto toDto(Listing l) {
        return ListingDto.builder()
                .id(l.getId())
                .type(l.getType())
                .status(l.getStatus())
                .statusEmoji(l.getStatus().getEmoji())
                .district(l.getDistrict())
                .rooms(l.getRooms())
                .price(l.getPrice())
                .utilitiesIncluded(l.isUtilitiesIncluded())
                .furniture(l.isFurniture())
                .appliances(l.isAppliances())
                .tenantType(Gender.valueOf(l.getTenantType()))
                .spotsAvailable(l.getSpotsAvailable())
                .preferredGender(l.getPreferredGender())
                .contact(l.getContact())
                .description(l.getDescription())
                .photoFileIds(l.getPhotoFileIds())
                .expiresAt(l.getExpiresAt())
                .createdAt(l.getCreatedAt())
                .userId(l.getUser().getId())
                .build();
    }
}
