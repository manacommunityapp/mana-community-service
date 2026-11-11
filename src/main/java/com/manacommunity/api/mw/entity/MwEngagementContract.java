package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_engagement_contracts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwEngagementContract {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private MwPoolApplication application;

    @Column(columnDefinition = "TEXT")
    private String termsSummary;

    private Boolean workerConsented;
    private LocalDateTime consentedAt;
    private BigDecimal hoursWorked;
    private BigDecimal amountDue;
    private String status;
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (status == null) status = "ACTIVE";
        if (workerConsented == null) workerConsented = false;
    }
}
