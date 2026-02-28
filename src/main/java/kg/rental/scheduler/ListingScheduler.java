package kg.rental.scheduler;

import kg.rental.config.AppConfig;
import kg.rental.entity.Listing;
import kg.rental.service.ListingService;
import kg.rental.service.TelegramChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListingScheduler {

    private final ListingService listingService;
    private final TelegramChannelService channelService;
    private final AppConfig appConfig;

    /**
     * Каждые 6 часов — отправить напоминания хозяевам объявлений старше 3 дней
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void sendReminders() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(appConfig.getListingReminderDays());
        List<Listing> listings = listingService.findNeedingReminder(threshold);

        log.info("Sending reminders for {} listings", listings.size());

        for (Listing listing : listings) {
            try {
                channelService.sendReminder(
                        listing.getUser().getTelegramId(),
                        listing.getId()
                );
                listingService.markReminderSent(listing.getId());
                channelService.updateListingStatus(listing);
            } catch (Exception e) {
                log.error("Failed to send reminder for listing {}: {}", listing.getId(), e.getMessage());
            }
        }
    }

    /**
     * Каждый час — архивировать просроченные объявления
     */
    @Scheduled(cron = "0 0 * * * *")
    public void archiveExpired() {
        List<Listing> expired = listingService.findExpired();

        log.info("Archiving {} expired listings", expired.size());

        for (Listing listing : expired) {
            try {
                listingService.archiveListing(listing.getId());
                channelService.updateListingStatus(listing);
                // Уведомить хозяина
                channelService.sendReminder(
                        listing.getUser().getTelegramId(),
                        listing.getId()
                );
            } catch (Exception e) {
                log.error("Failed to archive listing {}: {}", listing.getId(), e.getMessage());
            }
        }
    }
}
