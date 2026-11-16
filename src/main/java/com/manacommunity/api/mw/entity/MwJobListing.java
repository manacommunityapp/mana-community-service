package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_job_listings")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwJobListing {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 30)
    private String employmentType;

    @Column(length = 200)
    private String location;

    private Boolean isRemote;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;

    @Column(columnDefinition = "TEXT")
    private String requiredSkills;

    private Integer experienceYears;

    @Column(length = 20)
    private String status;

    private LocalDate expiresAt;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (isRemote == null) isRemote = false;
        if (employmentType == null) employmentType = "FULL_TIME";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
