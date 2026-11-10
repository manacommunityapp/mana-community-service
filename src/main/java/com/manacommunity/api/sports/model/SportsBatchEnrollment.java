package com.manacommunity.api.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_batch_enrollment",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_batch_student", columnNames = {"batch_id", "student_id"})
    },
    indexes = {
        @Index(name = "idx_batch_enrollment_student", columnList = "student_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsBatchEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnore
    private SportsBatch batch;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 150)
    private String studentName;

    @Column(name = "parent_name", length = 150)
    private String parentName;

    @Column(name = "parent_phone", nullable = false, length = 30)
    private String parentPhone;

    @Column(name = "flat_no", length = 50)
    private String flatNo;

    @Column(name = "community_name", length = 150)
    private String communityName;

    @Column(name = "joined_date", nullable = false)
    @Builder.Default
    private LocalDate joinedDate = LocalDate.now();

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "fee_paid_until")
    private LocalDate feePaidUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
