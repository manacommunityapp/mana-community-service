package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.CareJourneyStage;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_care_journey", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCareJourney extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "care_journey_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID careJourneyUuid = UUID.randomUUID();

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_uuid")
    private UUID appointmentUuid;

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false, length = 40)
    @Builder.Default
    private CareJourneyStage currentStage = CareJourneyStage.APPOINTMENT_BOOKED;

    @Column(name = "appointment_booked_at")
    private LocalDateTime appointmentBookedAt;

    @Column(name = "consultation_completed_at")
    private LocalDateTime consultationCompletedAt;

    @Column(name = "prescription_issued_at")
    private LocalDateTime prescriptionIssuedAt;

    @Column(name = "lab_test_ordered_at")
    private LocalDateTime labTestOrderedAt;

    @Column(name = "results_ready_at")
    private LocalDateTime resultsReadyAt;

    @Column(name = "follow_up_scheduled_at")
    private LocalDateTime followUpScheduledAt;
}
