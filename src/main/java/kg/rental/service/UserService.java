package kg.rental.service;

import kg.rental.entity.User;
import kg.rental.entity.UserDraft;
import kg.rental.enums.UserState;
import kg.rental.repository.UserDraftRepository;
import kg.rental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserDraftRepository userDraftRepository;

    public User getByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("User not found: " + telegramId));
    }
    @Transactional
    public User getOrCreate(org.telegram.telegrambots.meta.api.objects.User tgUser) {
        return userRepository.findByTelegramId(tgUser.getId())
                .orElseGet(() -> {
                    User user = User.builder()
                            .telegramId(tgUser.getId())
                            .username(tgUser.getUserName())
                            .firstName(tgUser.getFirstName())
                            .build();
                    return userRepository.save(user);
                });
    }

    @Transactional
    public void setState(Long telegramId, UserState state) {
        userRepository.findByTelegramId(telegramId).ifPresent(user -> {
            user.setState(state);
            userRepository.save(user);
        });
    }

    public UserState getState(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .map(User::getState)
                .orElse(UserState.IDLE);
    }

    // Черновик анкеты
    @Transactional
    public void saveDraftField(Long userId, String key, Object value) {
        UserDraft draft = userDraftRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow();
                    return UserDraft.builder().user(user).data(new HashMap<>()).build();
                });
        draft.getData().put(key, value);
        userDraftRepository.save(draft);
    }

    public Map<String, Object> getDraft(Long userId) {
        return userDraftRepository.findByUserId(userId)
                .map(UserDraft::getData)
                .orElse(new HashMap<>());
    }

    @Transactional
    public void clearDraft(Long userId) {
        userDraftRepository.findByUserId(userId).ifPresent(draft -> {
            draft.setData(new HashMap<>());
            userDraftRepository.save(draft);
        });
    }
}
