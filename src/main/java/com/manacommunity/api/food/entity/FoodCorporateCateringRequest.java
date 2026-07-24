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
@Table(name = "food_corporate_catering_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodCorporateCateringRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private FoodCorporateAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private AppUser requestedBy;

    @Column(name = "event_name", length = 200)
    private String eventName;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_time")
    private LocalTime eventTime;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Column(precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(name = "menu_preferences", columnDefinition = "TEXT")
    private String menuPreferences;

    @Column(name = "dietary_requirements", columnDefinition = "TEXT")
    private String dietaryRequirements;

    @Column(length = 500)
    private String venue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CorporateCateringStatus status = CorporateCateringStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum CorporateCateringStatus { PENDING, QUOTED, APPROVED, CONFIRMED, COMPLETED, CANCELLED }
}
