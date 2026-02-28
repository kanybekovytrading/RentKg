package kg.rental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RentalBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(RentalBotApplication.class, args);
    }
}
