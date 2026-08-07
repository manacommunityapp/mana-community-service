package com.manacommunity.api.events.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_event_meal_registration", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id", "meal_date", "meal_type"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private CommunityEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "meal_date", nullable = false)
    private LocalDate mealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 10)
    private MealType mealType;

    @Column(name = "head_count")
    @Builder.Default
    private Integer headCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_pref", length = 20)
    @Builder.Default
    private DietaryPref dietaryPref = DietaryPref.VEG;

    @Column(length = 500)
    private String allergies;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum MealType { LUNCH, DINNER }
    public enum DietaryPref { VEG, VEGAN, JAIN, NONVEG }
}
