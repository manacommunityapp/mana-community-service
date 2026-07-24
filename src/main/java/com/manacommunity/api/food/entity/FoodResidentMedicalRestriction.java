package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_resident_medical_restrictions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodResidentMedicalRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FoodResidentProfile profile;

    @Column(name = "restriction_name", nullable = false, length = 200)
    private String restrictionName;

    @Column(name = "prescribed_by", length = 200)
    private String prescribedBy;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
