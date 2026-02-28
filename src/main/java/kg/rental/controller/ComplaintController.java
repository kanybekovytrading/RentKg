package kg.rental.controller;

import kg.rental.dto.ApiResponse;
import kg.rental.dto.ComplaintRequest;
import kg.rental.entity.Listing;
import kg.rental.exception.ResourceNotFoundException;
import kg.rental.repository.ListingRepository;
import kg.rental.service.ComplaintService;
import kg.rental.service.TelegramChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final ListingRepository listingRepository;
    private final TelegramChannelService channelService;

    /**
     * POST /api/complaints
     * Подать жалобу на объявление
     */
    @PostMapping
    public ApiResponse<Void> submitComplaint(@RequestBody ComplaintRequest request) {
        boolean banned = complaintService.submitComplaint(
                request.getListingId(),
                request.getReporterTelegramId(),
                request.getReason()
        );

        if (banned) {
            Listing listing = listingRepository.findById(request.getListingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Объявление", request.getListingId()));
            channelService.publishBlacklistWarning(listing);
            channelService.updateListingStatus(listing);
            return ApiResponse.ok("Объявление заблокировано — набрано 3 жалобы", null);
        }

        return ApiResponse.ok("Жалоба принята", null);
    }
}
