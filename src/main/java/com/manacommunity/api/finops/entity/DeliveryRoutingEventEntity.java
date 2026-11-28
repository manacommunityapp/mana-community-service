package com.manacommunity.api.finops.entity;

import com.manacommunity.api.finops.dto.FinOpsDtos.EscalationStepDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "delivery_routing_events", schema = "finops")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryRoutingEventEntity {

    @Id
    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "resident_id", nullable = false, length = 100)
    private String residentId;

    @Column(name = "visitor_type", nullable = false, length = 50)
    private String visitorType;

    @Column(name = "sla_ms", nullable = false)
    private int slaMs = 15_000;

    @Column(name = "selected_initial_channel", nullable = false, length = 30)
    private String selectedInitialChannel;

    @Column(name = "current_channel", nullable = false, length = 30)
    private String currentChannel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "escalation_path_json", columnDefinition = "jsonb", nullable = false)
    private List<EscalationStepDto> escalationPathJson;

    @Column(name = "winning_channel", length = 30)
    private String winningChannel;

    @Column(name = "resolved_latency_ms")
    private Integer resolvedLatencyMs;

    @Column(name = "sla_met")
    private Boolean slaMet;

    @Column(name = "total_cost_paise", nullable = false)
    private int totalCostPaise = 0;

    @Column(name = "baseline_ivr_cost_paise", nullable = false)
    private int baselineIvrCostPaise = 40;

    @Column(name = "savings_paise", nullable = false)
    private int savingsPaise = 40;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "routing_reason", length = 400)
    private String routingReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
