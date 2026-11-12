package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mw_job_applications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "mana_user_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwJobApplication {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private MwJobListing job;

    private Long manaUserId;
    private Long passportId;

    @Column(columnDefinition = "TEXT")
    private String coverNote;

    @Column(length = 30)
    private String status;

    private LocalDateTime appliedAt;
    private LocalDateTime respondedAt;

    @Column(length = 500)
    private String employerNote;

    @PrePersist
    void onCreate() {
        appliedAt = LocalDateTime.now();
        if (status == null) status = "APPLIED";
    }
}
