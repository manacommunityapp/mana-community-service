package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.EmergencyCallbackStatus;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_emergency_callback", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthEmergencyCallback extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "callback_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID callbackUuid = UUID.randomUUID();

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "caller_name", length = 120)
    private String callerName;

    @Column(name = "caller_phone", length = 20, nullable = false)
    private String callerPhone;

    @Column(name = "emergency_description", length = 1000)
    private String emergencyDescription;

    @Column(name = "address", length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EmergencyCallbackStatus status = EmergencyCallbackStatus.REQUESTED;

    @Column(name = "assigned_agent_name", length = 100)
    private String assignedAgentName;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
