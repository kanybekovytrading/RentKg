package kg.rental.entity;

import jakarta.persistence.*;
import kg.rental.enums.Gender;
import kg.rental.enums.ListingStatus;
import kg.rental.enums.ListingType;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ListingStatus status = ListingStatus.ACTIVE;

    @Column(nullable = false)
    private String district;

    private Integer rooms;
    private Integer price;

    @Column(name = "price_range")
    private String priceRange;

    @Column(name = "utilities_included")
    @Builder.Default
    private boolean utilitiesIncluded = false;

    @Builder.Default
    private boolean furniture = false;

    @Builder.Default
    private boolean appliances = false;

    /**
     * Для кого сдаётся — теперь String через запятую, т.к. можно выбрать несколько.
     * Пример: "FEMALE,FAMILY" или "ANY"
     */
    @Column(name = "tenant_type", columnDefinition = "varchar(100)")
    @Builder.Default
    private String tenantType = "ANY";

    @Column(name = "spots_available")
    private Integer spotsAvailable;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_gender", columnDefinition = "varchar(20)")
    @Builder.Default
    private Gender preferredGender = Gender.ANY;

    /**
     * Пол самого ищущего подселение (для ROOMMATE_SEEK).
     * Используется при матчинге чтобы понять совместимость.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "my_gender", columnDefinition = "varchar(20)")
    private Gender myGender;

    @Column(name = "offer_room_type", length = 10)
    private String offerRoomType;

    private String contact;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_file_ids", columnDefinition = "TEXT[]")
    private String[] photoFileIds;

    @Column(name = "main_channel_msg_id")
    private Integer mainChannelMsgId;

    @Column(name = "niche_channel_msg_id")
    private Integer nicheChannelMsgId;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}