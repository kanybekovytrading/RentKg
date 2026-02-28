package kg.rental.repository;

import kg.rental.entity.Listing;
import kg.rental.enums.ListingStatus;
import kg.rental.enums.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Listing> {

    List<Listing> findByStatus(ListingStatus status);

    // Найти объявления для напоминания (3 дня без подтверждения, ещё не отправляли)
    @Query("""
        SELECT l FROM Listing l
        WHERE l.status = 'ACTIVE'
          AND l.reminderSentAt IS NULL
          AND l.createdAt < :threshold
        """)
    List<Listing> findListingsNeedingReminder(@Param("threshold") LocalDateTime threshold);

    // Найти просроченные объявления
    @Query("""
        SELECT l FROM Listing l
        WHERE l.status IN ('ACTIVE', 'PENDING')
          AND l.expiresAt < :now
        """)
    List<Listing> findExpiredListings(@Param("now") LocalDateTime now);

    // Найти объявления для уведомлений по подписке
    @Query("""
        SELECT l FROM Listing l
        WHERE l.type = :type
          AND l.status = 'ACTIVE'
          AND (:district IS NULL OR l.district = :district)
          AND (:maxPrice IS NULL OR l.price <= :maxPrice)
          AND (:rooms IS NULL OR l.rooms = :rooms)
          AND l.createdAt > :since
        """)
    List<Listing> findMatchingListings(
            @Param("type") ListingType type,
            @Param("district") String district,
            @Param("maxPrice") Integer maxPrice,
            @Param("rooms") Integer rooms,
            @Param("since") LocalDateTime since
    );

    List<Listing> findByUserTelegramIdAndStatusIn(Long telegramId, List<ListingStatus> statuses);
    /**
     * Находит активные объявления по типу и району — для матчинга встречных объявлений.
     */
    @Query("SELECT l FROM Listing l WHERE l.type = :type AND l.district = :district AND l.status = :status")
    List<Listing> findActiveByTypeAndDistrict(
            @Param("type") ListingType type,
            @Param("district") String district,
            @Param("status") ListingStatus status
    );

    @Query("SELECT l FROM Listing l JOIN FETCH l.user WHERE l.id = :id")
    Optional<Listing> findByIdWithUser(@Param("id") Long id);
}
