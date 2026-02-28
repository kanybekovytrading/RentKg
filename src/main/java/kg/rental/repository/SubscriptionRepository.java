package kg.rental.repository;

import kg.rental.entity.Subscription;
import kg.rental.enums.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("""
        SELECT s FROM Subscription s
        WHERE s.active = true
          AND s.listingType = :type
          AND (:district IS NULL OR s.district IS NULL OR s.district = :district)
        """)
    List<Subscription> findActiveByTypeAndDistrict(
            @Param("type") ListingType type,
            @Param("district") String district
    );

    List<Subscription> findByUserId(Long userId);
}
