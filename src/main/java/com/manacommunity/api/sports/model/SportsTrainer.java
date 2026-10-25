package com.manacommunity.api.sports.model;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_trainer", schema = "sports",
        indexes = {
                @Index(name = "idx_sports_trainer_community", columnList = "community_id"),
                @Index(name = "idx_sports_trainer_user", columnList = "user_id"),
                @Index(name = "idx_sports_trainer_sport", columnList = "sport"),
                @Index(name = "idx_sports_trainer_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsTrainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    /** The resident who applied */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 200)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String sport;

    @Column(name = "cert_body", length = 150)
    private String certBody;

    @Column(name = "cert_number", length = 100)
    private String certNumber;

    @Column(name = "cert_year")
    private Integer certYear;

    @Column(name = "age_groups_coached", length = 100)
    private String ageGroupsCoached;

    @Column(length = 1000)
    private String bio;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "profile_pic_url", length = 500)
    private String profilePicUrl;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "APPLIED";  // APPLIED | APPROVED | REJECTED | SUSPENDED

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
