package kg.rental.controller;

import kg.rental.dto.ApiResponse;
import kg.rental.entity.Blacklist;
import kg.rental.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistRepository blacklistRepository;

    /**
     * GET /api/blacklist
     * Получить весь чёрный список
     */
    @GetMapping
    public ApiResponse<List<Blacklist>> getBlacklist() {
        return ApiResponse.ok(blacklistRepository.findAll());
    }

    /**
     * GET /api/blacklist/check?telegramId=123456
     * Проверить есть ли пользователь в чёрном списке
     */
    @GetMapping("/check")
    public ApiResponse<Boolean> checkBlacklist(
            @RequestParam(required = false) Long telegramId,
            @RequestParam(required = false) String phone
    ) {
        boolean banned = false;
        if (telegramId != null) banned = blacklistRepository.existsByTelegramId(telegramId);
        if (!banned && phone != null) banned = blacklistRepository.existsByPhone(phone);
        return ApiResponse.ok(banned);
    }
}
