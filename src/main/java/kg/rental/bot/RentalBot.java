package kg.rental.bot;

import jakarta.annotation.PostConstruct;
import kg.rental.bot.handler.CallbackHandler;
import kg.rental.bot.handler.MessageHandler;
import kg.rental.config.TelegramConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
@Slf4j
public class RentalBot extends TelegramLongPollingBot {

    private final TelegramConfig config;
    private final MessageHandler messageHandler;
    private final CallbackHandler callbackHandler;

    public RentalBot(TelegramConfig config,
                     @Lazy MessageHandler messageHandler,
                     @Lazy CallbackHandler callbackHandler) {
        super(config.getBotToken());
        this.config = config;
        this.messageHandler = messageHandler;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    @PostConstruct
    public void registerCommands() {
        try {
            execute(SetMyCommands.builder()
                    .commands(List.of(
                            BotCommand.builder().command("menu").description("Показать меню").build(),
                            BotCommand.builder().command("start").description("Начать").build()
                    ))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to set commands: {}", e.getMessage());
        }
    }
    @Override
    public void onUpdateReceived(Update update) {
        log.info("=== UPDATE RECEIVED: {}", update);

        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                messageHandler.handle(update.getMessage());
            } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
                messageHandler.handlePhoto(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                callbackHandler.handle(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", e.getMessage(), e);
        }
    }
}
