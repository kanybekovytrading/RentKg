package kg.rental.entity;

import jakarta.persistence.*;
import kg.rental.enums.Gender;
import kg.rental.enums.ListingType;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    private String district;

    @Column(name = "max_budget")
    private Integer maxBudget;

    private Integer rooms;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Gender gender = Gender.ANY;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
