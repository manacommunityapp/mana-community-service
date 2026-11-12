package com.manacommunity.api.socialcircle.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kitty_cycle", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KittyPartyCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false)
    private KittyPartyCircle circle;

    @Column(name = "cycle_name", nullable = false, length = 150)
    private String cycleName;

    @Column(name = "contribution_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal contributionAmount;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String frequency = "MONTHLY"; // WEEKLY, MONTHLY, BI_WEEKLY

    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "financial_mode", nullable = false, length = 30)
    @Builder.Default
    private String financialMode = "RECORD_ONLY";

    @Column(name = "legal_consent_ver", nullable = false, length = 30)
    @Builder.Default
    private String legalConsentVer = "2026_V1";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, ACTIVE, COMPLETED, SUSPENDED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
