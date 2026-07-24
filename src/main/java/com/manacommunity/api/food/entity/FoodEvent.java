package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "food_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 30)
    private FoodEventType eventType;

    @Column(length = 500)
    private String venue;

    @Column
    private LocalDate date;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column
    private Integer capacity;

    @Column
    @Builder.Default
    private Integer registered = 0;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private AppUser organizer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FoodEventStatus status;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum FoodEventType { POTLUCK, FESTIVAL, COOKING_COMPETITION, WINE_TASTING, BBQ, KIDS_COOKING, RECIPE_CONTEST, FOOD_EXHIBITION }

    public enum FoodEventStatus { DRAFT, PUBLISHED, ONGOING, COMPLETED, CANCELLED }
}
