package com.manacommunity.api.healthcare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "diagnostics_sla")
@Data
public class DiagnosticsSla {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "partner_id", nullable = false) private Long partnerId;
    @Column(name = "partner_name", nullable = false, length = 300) private String partnerName;
    @Column(name = "test_category", nullable = false, length = 200) private String testCategory;
    @Column(name = "sla_hours", nullable = false) private Integer slaHours;
    @Column(name = "report_due_at", nullable = false) private Instant reportDueAt;
    @Column(name = "report_delivered_at") private Instant reportDeliveredAt;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "patient_user_id", nullable = false) private Long patientUserId;
    @Column private Boolean breach = false;
    @Column(name = "breach_notified") private Boolean breachNotified = false;
    @Column private Boolean escalated = false;
    @Column(name = "escalated_at") private Instant escalatedAt;
    @Column(length = 1000) private String notes;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
