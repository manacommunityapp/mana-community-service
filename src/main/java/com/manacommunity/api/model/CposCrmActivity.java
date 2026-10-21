package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cpos_crm_activity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CposCrmActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private CposCrmLead lead;

    @Column(name = "activity_type", length = 50, nullable = false)
    private String activityType; // CALL, VISIT, NOTE, OFFER, EMAIL

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "call_outcome", length = 20)
    private String callOutcome; // CONNECTED | MISSED | BUSY — only for CALL activities

    @Column(name = "call_duration_min")
    private Integer callDurationMin;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
