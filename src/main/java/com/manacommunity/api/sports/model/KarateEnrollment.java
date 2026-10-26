package com.manacommunity.api.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "karate_enrollment",
        uniqueConstraints = @UniqueConstraint(name = "uq_karate_enrollment", columnNames = {"batch_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KarateEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private KarateBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_belt_id")
    private KarateBelt currentBelt;

    @Column(nullable = false, length = 20)
    private String status;             // ACTIVE, WITHDRAWN, COMPLETED

    @Column(name = "classes_attended", nullable = false)
    private Integer classesAttended = 0;

    @Column(name = "total_classes", nullable = false)
    private Integer totalClasses = 0;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        enrolledAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (classesAttended == null) classesAttended = 0;
        if (totalClasses == null) totalClasses = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
