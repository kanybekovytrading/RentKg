package kg.rental.controller;

import kg.rental.dto.ApiResponse;
import kg.rental.dto.ListingDto;
import kg.rental.dto.ListingMapper;
import kg.rental.dto.PageResponse;
import kg.rental.entity.Listing;
import kg.rental.enums.ListingStatus;
import kg.rental.enums.ListingType;
import kg.rental.exception.ResourceNotFoundException;
import kg.rental.repository.ListingRepository;
import kg.rental.service.ListingService;
import kg.rental.service.TelegramChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingRepository listingRepository;
    private final ListingService listingService;
    private final TelegramChannelService channelService;
    private final ListingMapper mapper;

    /**
     * GET /api/listings?type=RENT_OUT&district=Аламедин&page=0&size=20
     * Получить список объявлений с фильтрацией
     */
    @GetMapping
    public ApiResponse<PageResponse<ListingDto>> getListings(
            @RequestParam(required = false) ListingType type,
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer rooms,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Specification<Listing> spec = Specification.where(null);

        if (type != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("type"), type));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        } else {
            // По умолчанию — только активные
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), ListingStatus.ACTIVE));
        }
        if (district != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("district"), district));
        }
        if (minPrice != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        if (rooms != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("rooms"), rooms));
        }

        Page<Listing> result = listingRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        List<ListingDto> dtos = result.getContent().stream().map(mapper::toDto).toList();

        return ApiResponse.ok(PageResponse.<ListingDto>builder()
                .content(dtos)
                .page(page)
                .size(size)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build());
    }

    /**
     * GET /api/listings/{id}
     * Получить объявление по ID
     */
    @GetMapping("/{id}")
    public ApiResponse<ListingDto> getListing(@PathVariable Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление", id));
        return ApiResponse.ok(mapper.toDto(listing));
    }

    /**
     * PATCH /api/listings/{id}/confirm
     * Подтвердить актуальность объявления (хозяин)
     */
    @PatchMapping("/{id}/confirm")
    public ApiResponse<Void> confirmListing(@PathVariable Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление", id));
        listingService.confirmListing(id);
        channelService.updateListingStatus(listing);
        return ApiResponse.ok("Объявление подтверждено", null);
    }

    /**
     * PATCH /api/listings/{id}/close
     * Закрыть объявление (сдано/найдено)
     */
    @PatchMapping("/{id}/close")
    public ApiResponse<Void> closeListing(@PathVariable Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Объявление", id));
        listingService.closeListing(id);
        channelService.updateListingStatus(listing);
        return ApiResponse.ok("Объявление закрыто", null);
    }

    /**
     * DELETE /api/listings/{id}
     * Удалить объявление (только для admin)
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteListing(@PathVariable Long id) {
        if (!listingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Объявление", id);
        }
        listingRepository.deleteById(id);
        return ApiResponse.ok("Объявление удалено", null);
    }
}
