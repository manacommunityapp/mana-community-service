package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.AppointmentStatus;
import com.manacommunity.api.health.enums.HealthEnums.ConsultationMode;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "health_appointment", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthAppointment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "appointment_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID appointmentUuid = UUID.randomUUID();

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", insertable = false, updatable = false)
    private HealthDoctor doctor;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "patient_name", length = 150)
    private String patientName;

    @Column(name = "booked_by_user_id", nullable = false)
    private Long bookedByUserId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "slot_start_time", nullable = false)
    private LocalTime slotStartTime;

    @Column(name = "slot_end_time", nullable = false)
    private LocalTime slotEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 30)
    @Builder.Default
    private ConsultationMode mode = ConsultationMode.IN_PERSON;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(name = "payment_reference_id", length = 100)
    private String paymentReferenceId;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "symptoms_summary", columnDefinition = "TEXT")
    private String symptomsSummary;
}
