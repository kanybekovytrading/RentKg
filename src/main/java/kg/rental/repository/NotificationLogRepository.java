package kg.rental.repository;

import kg.rental.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    @Query("""
        SELECT COUNT(n) FROM NotificationLog n
        WHERE n.user.id = :userId
          AND n.sentAt > :since
        """)
    long countTodayNotifications(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    boolean existsByUserIdAndListingId(Long userId, Long listingId);
}
