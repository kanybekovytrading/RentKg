package kg.rental.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class TelegramConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    // Числовой ID супергруппы, например -1001234567890
    @Value("${telegram.channels.main}")
    private String mainChannel;

    // Thread ID каждой темы внутри группы
    @Value("${telegram.threads.rent-out}")
    private Integer threadRentOut;

    @Value("${telegram.threads.rent-in}")
    private Integer threadRentIn;

    @Value("${telegram.threads.roommate}")
    private Integer threadRoommate;

    @Value("${telegram.threads.blacklist}")
    private Integer threadBlacklist;

    @Value("${telegram.threads.need-roommate}")
    private Integer threadNeedRoommate;

    @Value("${telegram.threads.commercial-room}")
    private Integer threadCommercialRoom;
}
