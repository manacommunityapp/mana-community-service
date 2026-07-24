package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "food_dining_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodDiningReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", nullable = false, length = 20)
    private ReservationType reservationType;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(length = 200)
    private String occasion;

    @Column(name = "pre_order_id")
    private Long preOrderId;

    @Column(name = "confirmation_code", length = 50)
    private String confirmationCode;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum ReservationType { RESTAURANT, CLUBHOUSE, PARTY_HALL, POOLSIDE, CHEF_TABLE }

    public enum ReservationStatus { PENDING, CONFIRMED, SEATED, COMPLETED, CANCELLED, NO_SHOW }
}
