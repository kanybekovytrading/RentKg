package kg.rental.entity;

import jakarta.persistence.*;
import kg.rental.enums.UserState;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", unique = true, nullable = false)
    private Long telegramId;

    private String username;

    @Column(name = "first_name")
    private String firstName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserState state = UserState.IDLE;

    @Column(nullable = false)
    @Builder.Default
    private String language = "ru";

    @Column(name = "is_banned")
    @Builder.Default
    private boolean banned = false;

    @Column(name = "notifications_enabled")
    @Builder.Default
    private boolean notificationsEnabled = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime bannedUntil;

    // В методе isBanned() добавьте проверку:
    public boolean isBanned() {
        return banned || (bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now()));
    }
}

