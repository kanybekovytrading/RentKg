package kg.rental.service;

import kg.rental.config.AppConfig;
import kg.rental.entity.Blacklist;
import kg.rental.entity.Complaint;
import kg.rental.entity.Listing;
import kg.rental.entity.User;
import kg.rental.enums.ComplaintReason;
import kg.rental.enums.ListingStatus;
import kg.rental.repository.BlacklistRepository;
import kg.rental.repository.ComplaintRepository;
import kg.rental.repository.ListingRepository;
import kg.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final BlacklistRepository blacklistRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final AppConfig appConfig;

    /**
     * @return true если порог жалоб превышен и объявление заблокировано
     */
    @Transactional
    public boolean submitComplaint(Long listingId, Long reporterTelegramId, ComplaintReason reason) {
        log.info("=== submitComplaint: listingId={}, reporter={}, reason={}", listingId, reporterTelegramId, reason);

        Listing listing = listingRepository.findById(listingId).orElseThrow();
        User reporter = userRepository.findByTelegramId(reporterTelegramId).orElse(null);
        if (reporter == null) return false;

        if (listing.getUser().getTelegramId().equals(reporterTelegramId)) {
            log.warn("=== own listing");
            return false;
        }

        // ALREADY_RENTED — закрываем сразу, без проверки на повтор
        if (reason == ComplaintReason.ALREADY_RENTED) {
            listing.setStatus(ListingStatus.CLOSED);
            listingRepository.save(listing);
            return false;
        }

        // Для остальных причин — проверяем повтор
        if (complaintRepository.existsByListingIdAndReporterId(listingId, reporter.getId())) {
            log.warn("=== already complained");
            return false;
        }

        Complaint complaint = Complaint.builder()
                .listing(listing)
                .reporter(reporter)
                .reason(reason)
                .build();
        complaintRepository.save(complaint);
        log.info("=== complaint saved");

        if (reason == ComplaintReason.SCAMMER || reason == ComplaintReason.PHOTO_MISMATCH) {
            long count = complaintRepository.countByListingIdAndReasonIn(
                    listingId, List.of(ComplaintReason.SCAMMER, ComplaintReason.PHOTO_MISMATCH));
            log.info("=== banCount={}, threshold={}", count, appConfig.getComplaintThreshold());

            if (count >= appConfig.getComplaintThreshold()) {
                banListing(listing);
                return true;
            }
        }

        return false;
    }
    private void banListing(Listing listing) {
        listing.setStatus(ListingStatus.ARCHIVED);
        listingRepository.save(listing);

        User owner = listing.getUser();

        // Ограничить на 7 дней
        owner.setBannedUntil(LocalDateTime.now().plusDays(7));
        userRepository.save(owner);

        Blacklist entry = Blacklist.builder()
                .telegramId(owner.getTelegramId())
                .username(owner.getUsername())
                .phone(owner.getPhone())
                .reason("3 жалобы на мошенничество")
                .listing(listing)
                .build();
        blacklistRepository.save(entry);

        log.warn("Listing {} banned, user {} restricted for 7 days",
                listing.getId(), owner.getTelegramId());
    }

    public boolean isBlacklisted(Long telegramId) {
        return blacklistRepository.existsByTelegramId(telegramId);
    }
}
