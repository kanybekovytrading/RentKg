package kg.rental.controller;

import kg.rental.dto.ApiResponse;
import kg.rental.enums.ListingStatus;
import kg.rental.enums.ListingType;
import kg.rental.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final BlacklistRepository blacklistRepository;
    private final ComplaintRepository complaintRepository;

    /**
     * GET /api/stats
     * Общая статистика
     */
    @GetMapping
    public ApiResponse<Stats> getStats() {
        Stats stats = Stats.builder()
                .totalUsers(userRepository.count())
                .totalListings(listingRepository.count())
                .activeListings(listingRepository.findByStatus(ListingStatus.ACTIVE).size())
                .pendingListings(listingRepository.findByStatus(ListingStatus.PENDING).size())
                .rentOutListings(countByType(ListingType.RENT_OUT))
                .rentInListings(countByType(ListingType.RENT_IN))
                .roommateListings(countByType(ListingType.ROOMMATE_SEEK) + countByType(ListingType.ROOMMATE_OFFER))
                .blacklistedUsers(blacklistRepository.count())
                .totalComplaints(complaintRepository.count())
                .build();
        return ApiResponse.ok(stats);
    }

    private long countByType(ListingType type) {
        return listingRepository.findAll().stream()
                .filter(l -> l.getType() == type && l.getStatus() == ListingStatus.ACTIVE)
                .count();
    }

    @Data @Builder
    public static class Stats {
        private long totalUsers;
        private long totalListings;
        private long activeListings;
        private long pendingListings;
        private long rentOutListings;
        private long rentInListings;
        private long roommateListings;
        private long blacklistedUsers;
        private long totalComplaints;
    }
}
