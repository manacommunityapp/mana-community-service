package com.manacommunity.api.childsecurity.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exit_escalation_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EscalationLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long exitRequestId;
    private Long guardianId;
    private String guardianName;
    private String guardianPhone;

    @Enumerated(EnumType.STRING)
    private EscalationMethod method;

    @Enumerated(EnumType.STRING)
    private EscalationOutcome outcome;

    private LocalDateTime initiatedAt;
    private LocalDateTime respondedAt;
    private int responseTimeSeconds;

    private int attemptNumber;
    private String ivrCallSid;
    private String remarks;

    public enum EscalationMethod { PUSH_NOTIFICATION, SMS, IVR_CALL, WHATSAPP, IN_APP_ALERT }

    public enum EscalationOutcome { PENDING, APPROVED, DENIED, NO_RESPONSE, CALL_FAILED, VOICEMAIL }
}
