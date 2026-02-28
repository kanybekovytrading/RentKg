package kg.rental.service;

import kg.rental.entity.Subscription;
import kg.rental.entity.User;
import kg.rental.enums.ListingType;
import kg.rental.repository.SubscriptionRepository;
import kg.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public List<Subscription> getUserSubscriptions(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId).orElseThrow();
        return subscriptionRepository.findByUserId(user.getId());
    }

    @Transactional
    public Subscription subscribe(Long telegramId, ListingType type, String district) {
        User user = userRepository.findByTelegramId(telegramId).orElseThrow();
        for (Subscription s : subscriptionRepository.findByUserId(user.getId())) {
            if (s.getListingType() == type && Objects.equals(s.getDistrict(), district)) {
                s.setActive(true);
                return subscriptionRepository.save(s);
            }
        }
        return subscriptionRepository.save(Subscription.builder()
                .user(user)
                .listingType(type)
                .district(district)
                .active(true)
                .build());
    }

    @Transactional
    public void unsubscribe(Long subscriptionId) {
        subscriptionRepository.findById(subscriptionId).ifPresent(s -> {
            s.setActive(false);
            subscriptionRepository.save(s);
        });
    }

    @Transactional
    public boolean toggleNotifications(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId).orElseThrow();
        boolean newState = !user.isNotificationsEnabled();
        user.setNotificationsEnabled(newState);
        userRepository.save(user);
        return newState;
    }
}