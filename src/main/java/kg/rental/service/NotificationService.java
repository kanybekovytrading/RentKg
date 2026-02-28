package kg.rental.service;

import kg.rental.config.AppConfig;
import kg.rental.entity.*;
import kg.rental.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final UserRepository userRepository;
    private final AppConfig appConfig;

    /**
     * Найти подходящих подписчиков для нового объявления и вернуть их telegram IDs
     */
    public List<Long> findSubscribers(Listing listing) {
        List<Subscription> subs = subscriptionRepository.findActiveByTypeAndDistrict(
                listing.getType(), listing.getDistrict()
        );

        return subs.stream()
                .filter(sub -> !sub.getUser().getTelegramId().equals(listing.getUser().getTelegramId()))
                .filter(sub -> sub.getUser().isNotificationsEnabled())
                .filter(sub -> !sub.getUser().isBanned())
                .filter(sub -> canReceiveNotification(sub.getUser().getId()))
                .filter(sub -> !notificationLogRepository.existsByUserIdAndListingId(
                        sub.getUser().getId(), listing.getId()))
                .map(sub -> sub.getUser().getTelegramId())
                .distinct()
                .toList();
    }

    @Transactional
    public void logNotification(Long userId, Long listingId) {
        User user = userRepository.findById(userId).orElseThrow();
        NotificationLog log = NotificationLog.builder()
                .user(user)
                // listing proxy — достаточно ID
                .build();
        // используем simple approach через native query если нужен только ID
        notificationLogRepository.save(log);
    }

    private boolean canReceiveNotification(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long count = notificationLogRepository.countTodayNotifications(userId, startOfDay);
        return count < appConfig.getMaxNotificationsPerDay();
    }

    @Transactional
    public void saveSubscription(Long telegramId, Subscription subscription) {
        subscriptionRepository.save(subscription);
    }
}
