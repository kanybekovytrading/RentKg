package kg.rental.service;

import kg.rental.entity.Listing;
import kg.rental.enums.ListingStatus;
import kg.rental.enums.ListingType;
import kg.rental.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Находит встречные объявления при публикации нового.
 *
 * Логика матчинга:
 *  RENT_OUT      ↔  RENT_IN:       район + комнаты + цена в бюджет
 *  ROOMMATE_OFFER ↔  ROOMMATE_SEEK: район + цена в бюджет + совместимость по полу
 *  RENT_ROOM_IN  ↔  ROOMMATE_OFFER (только тип ROOM): район + цена в бюджет
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final ListingRepository listingRepository;

    public List<Long> findMatches(Listing newListing) {
        return switch (newListing.getType()) {
            case RENT_OUT      -> matchRentOut(newListing);
            case RENT_IN       -> matchRentIn(newListing);
            case RENT_ROOM_IN  -> matchRentRoomIn(newListing);
            case ROOMMATE_OFFER -> matchRoommateOffer(newListing);
            case ROOMMATE_SEEK  -> matchRoommateSeek(newListing);
            case COMMERCIAL_RENT_OUT -> List.of();
        };
    }

    // ── RENT_OUT опубликован — ищем RENT_IN в этом районе ──

    private List<Long> matchRentOut(Listing offer) {
        List<Listing> seekers = listingRepository.findActiveByTypeAndDistrict(
                ListingType.RENT_IN, offer.getDistrict(), ListingStatus.ACTIVE);

        List<Long> result = new ArrayList<>();
        for (Listing seeker : seekers) {
            if (isSameUser(offer, seeker)) continue;
            if (!roomsMatch(offer.getRooms(), seeker.getRooms())) continue;
            if (!priceInBudget(offer.getPrice(), seeker.getPriceRange())) continue;
            result.add(seeker.getUser().getTelegramId());
        }
        return result;
    }

    // ── RENT_IN опубликован — ищем подходящие RENT_OUT ──

    private List<Long> matchRentIn(Listing seeker) {
        List<Listing> offers = listingRepository.findActiveByTypeAndDistrict(
                ListingType.RENT_OUT, seeker.getDistrict(), ListingStatus.ACTIVE);

        List<Long> result = new ArrayList<>();
        for (Listing offer : offers) {
            if (isSameUser(seeker, offer)) continue;
            if (!roomsMatch(offer.getRooms(), seeker.getRooms())) continue;
            if (!priceInBudget(offer.getPrice(), seeker.getPriceRange())) continue;
            result.add(offer.getUser().getTelegramId());
        }
        return result;
    }

    // ── RENT_ROOM_IN опубликован — ищем ROOMMATE_OFFER у кого сдаётся комната целиком ──

    private List<Long> matchRentRoomIn(Listing seeker) {
        List<Listing> offers = listingRepository.findActiveByTypeAndDistrict(
                ListingType.ROOMMATE_OFFER, seeker.getDistrict(), ListingStatus.ACTIVE);

        List<Long> result = new ArrayList<>();
        for (Listing offer : offers) {
            if (isSameUser(seeker, offer)) continue;
            // Матчим только тех кто сдаёт комнату целиком, не место в комнате
            if (!"ROOM".equals(offer.getOfferRoomType())) continue;
            if (!priceInBudget(offer.getPrice(), seeker.getPriceRange())) continue;
            result.add(offer.getUser().getTelegramId());
        }
        return result;
    }

    // ── ROOMMATE_OFFER опубликован — ищем ROOMMATE_SEEK + RENT_ROOM_IN если тип ROOM ──

    private List<Long> matchRoommateOffer(Listing offer) {
        List<Long> result = new ArrayList<>();

        // Ищем тех кто ищет подселение
        List<Listing> seekers = listingRepository.findActiveByTypeAndDistrict(
                ListingType.ROOMMATE_SEEK, offer.getDistrict(), ListingStatus.ACTIVE);
        for (Listing seeker : seekers) {
            if (isSameUser(offer, seeker)) continue;
            if (!priceInBudget(offer.getPrice(), seeker.getPriceRange())) continue;
            if (!genderCompatible(offer.getTenantType(), seeker.getMyGender())) continue;
            result.add(seeker.getUser().getTelegramId());
        }

        // Если сдаётся комната целиком — также ищем тех кто ищет комнату
        if ("ROOM".equals(offer.getOfferRoomType())) {
            List<Listing> roomSeekers = listingRepository.findActiveByTypeAndDistrict(
                    ListingType.RENT_ROOM_IN, offer.getDistrict(), ListingStatus.ACTIVE);
            for (Listing seeker : roomSeekers) {
                if (isSameUser(offer, seeker)) continue;
                if (!priceInBudget(offer.getPrice(), seeker.getPriceRange())) continue;
                result.add(seeker.getUser().getTelegramId());
            }
        }

        return result;
    }

    // ── ROOMMATE_SEEK опубликован — ищем подходящие ROOMMATE_OFFER ──

    private List<Long> matchRoommateSeek(Listing seeker) {
        List<Listing> offers = listingRepository.findActiveByTypeAndDistrict(
                ListingType.ROOMMATE_OFFER, seeker.getDistrict(), ListingStatus.ACTIVE);

        List<Long> result = new ArrayList<>();
        for (Listing offer : offers) {
            if (isSameUser(seeker, offer)) continue;
            if (!priceInBudget(offer.getPrice(), seeker.getPriceRange())) continue;
            if (!genderCompatible(offer.getTenantType(), seeker.getMyGender())) continue;
            result.add(offer.getUser().getTelegramId());
        }
        return result;
    }

    // ── helpers ──

    private boolean isSameUser(Listing a, Listing b) {
        return a.getUser().getTelegramId().equals(b.getUser().getTelegramId());
    }

    private boolean roomsMatch(Integer offerRooms, Integer seekerRooms) {
        if (offerRooms == null || seekerRooms == null) return true;
        return offerRooms.equals(seekerRooms);
    }

    /**
     * Проверяет что цена объявления попадает в диапазон бюджета ищущего.
     * Форматы: "до 10 000", "10 000 – 20 000", "от 30 000"
     */
    private boolean priceInBudget(Integer price, String priceRange) {
        if (price == null || priceRange == null || priceRange.isBlank()) return true;
        try {
            String cleaned = priceRange.replace(" ", "").replace("\u00a0", "");
            if (cleaned.startsWith("до")) {
                int max = Integer.parseInt(cleaned.replaceAll("[^0-9]", ""));
                return price <= max;
            } else if (cleaned.startsWith("от")) {
                int min = Integer.parseInt(cleaned.replaceAll("[^0-9]", ""));
                return price >= min;
            } else if (cleaned.contains("–")) {
                String[] parts = cleaned.split("–");
                int min = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                int max = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                return price >= min && price <= max;
            }
        } catch (Exception e) {
            log.warn("Cannot parse priceRange '{}': {}", priceRange, e.getMessage());
        }
        return true;
    }

    /**
     * Совместимость по полу: tenantType — строка через запятую ("FEMALE,FAMILY"),
     * seekerGender — пол ищущего (MALE / FEMALE).
     */
    private boolean genderCompatible(String tenantTypeStr, kg.rental.enums.Gender seekerGender) {
        if (tenantTypeStr == null || tenantTypeStr.isBlank() || seekerGender == null) return true;
        for (String part : tenantTypeStr.split(",")) {
            String t = part.trim();
            if (t.equals("ANY") || t.equals("FAMILY")) return true;
            if (t.equals(seekerGender.name())) return true;
        }
        return false;
    }
}