package kg.rental.service;

import kg.rental.config.AppConfig;
import kg.rental.entity.Listing;
import kg.rental.entity.User;
import kg.rental.enums.*;
import kg.rental.repository.ListingRepository;
import kg.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final AppConfig appConfig;

    @Transactional
    public Listing createFromDraft(Long telegramId, Map<String, Object> draft) {
        User user = userRepository.findByTelegramId(telegramId).orElseThrow();
        ListingType type = ListingType.valueOf((String) draft.get("type"));

        Listing listing = Listing.builder()
                .user(user)
                .type(type)
                .status(ListingStatus.ACTIVE)
                .district((String) draft.get("district"))
                .contact((String) draft.get("contact"))
                .description((String) draft.get("description"))
                .expiresAt(LocalDateTime.now().plusDays(appConfig.getListingExpiryDays()))
                .build();

        if (draft.containsKey("rooms"))
            listing.setRooms(toInt(draft.get("rooms")));
        if (draft.containsKey("price"))
            listing.setPrice(toInt(draft.get("price")));
        if (draft.containsKey("priceRange"))
            listing.setPriceRange((String) draft.get("priceRange"));
        if (draft.containsKey("utilitiesIncluded"))
            listing.setUtilitiesIncluded((boolean) draft.get("utilitiesIncluded"));
        if (draft.containsKey("furniture"))
            listing.setFurniture((boolean) draft.get("furniture"));
        if (draft.containsKey("appliances"))
            listing.setAppliances((boolean) draft.get("appliances"));

        // tenantType теперь String (например "FEMALE,FAMILY" или "ANY")
        if (draft.containsKey("tenantType"))
            listing.setTenantType((String) draft.get("tenantType"));

        if (draft.containsKey("spotsAvailable"))
            listing.setSpotsAvailable(toInt(draft.get("spotsAvailable")));
        if (draft.containsKey("preferredGender"))
            listing.setPreferredGender(Gender.valueOf((String) draft.get("preferredGender")));

        // Пол самого ищущего подселение
        if (draft.containsKey("myGender"))
            listing.setMyGender(Gender.valueOf((String) draft.get("myGender")));

        if (draft.containsKey("offerRoomType"))
            listing.setOfferRoomType((String) draft.get("offerRoomType"));
        if (draft.containsKey("photos")) {
            @SuppressWarnings("unchecked")
            List<String> photos = (List<String>) draft.get("photos");
            listing.setPhotoFileIds(photos.toArray(new String[0]));
        }

        return listingRepository.save(listing);
    }

    @Transactional
    public void confirmListing(Long listingId) {
        listingRepository.findById(listingId).ifPresent(l -> {
            l.setStatus(ListingStatus.ACTIVE);
            l.setConfirmedAt(LocalDateTime.now());
            l.setExpiresAt(LocalDateTime.now().plusDays(appConfig.getListingExpiryDays()));
            listingRepository.save(l);
        });
    }

    @Transactional
    public void closeListing(Long listingId) {
        listingRepository.findById(listingId).ifPresent(l -> {
            l.setStatus(ListingStatus.CLOSED);
            listingRepository.save(l);
        });
    }

    @Transactional
    public void markPending(Long listingId) {
        listingRepository.findById(listingId).ifPresent(l -> {
            l.setStatus(ListingStatus.PENDING);
            listingRepository.save(l);
        });
    }

    @Transactional
    public void archiveListing(Long listingId) {
        listingRepository.findById(listingId).ifPresent(l -> {
            l.setStatus(ListingStatus.ARCHIVED);
            listingRepository.save(l);
        });
    }

    @Transactional
    public void markReminderSent(Long listingId) {
        listingRepository.findById(listingId).ifPresent(l -> {
            l.setReminderSentAt(LocalDateTime.now());
            l.setStatus(ListingStatus.PENDING);
            listingRepository.save(l);
        });
    }

    @Transactional
    public void saveChannelMessageId(Long listingId, Integer msgId) {
        listingRepository.findById(listingId).ifPresent(l -> {
            l.setMainChannelMsgId(msgId);
            listingRepository.save(l);
        });
    }

    public Optional<Listing> findById(Long id) {
        return listingRepository.findById(id);
    }

    public List<Listing> findActiveByUser(Long telegramId) {
        return listingRepository.findByUserTelegramIdAndStatusIn(
                telegramId,
                List.of(ListingStatus.ACTIVE, ListingStatus.PENDING)
        );
    }

    public List<Listing> findNeedingReminder(LocalDateTime threshold) {
        return listingRepository.findListingsNeedingReminder(threshold);
    }

    public List<Listing> findExpired() {
        return listingRepository.findExpiredListings(LocalDateTime.now());
    }

    private int toInt(Object val) {
        if (val instanceof Integer i) return i;
        if (val instanceof Long l) return l.intValue();
        return Integer.parseInt(val.toString());
    }
}