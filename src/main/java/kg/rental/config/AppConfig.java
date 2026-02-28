package kg.rental.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AppConfig {

    @Value("${app.listing.expiry-days:7}")
    private int listingExpiryDays;

    @Value("${app.listing.reminder-days:3}")
    private int listingReminderDays;

    @Value("${app.notification.max-per-day:2}")
    private int maxNotificationsPerDay;

    @Value("${app.complaint.threshold:3}")
    private int complaintThreshold;
}
