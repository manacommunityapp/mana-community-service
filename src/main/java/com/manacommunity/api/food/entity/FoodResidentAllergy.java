package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_resident_allergies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodResidentAllergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FoodResidentProfile profile;

    @Column(name = "allergy_name", nullable = false, length = 200)
    private String allergyName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Severity { MILD, MODERATE, SEVERE }
}
