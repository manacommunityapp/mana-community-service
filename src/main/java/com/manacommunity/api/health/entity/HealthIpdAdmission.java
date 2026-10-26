package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.IpdAdmissionStatus;
import com.manacommunity.api.health.enums.HealthEnums.WardType;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_ipd_admission", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthIpdAdmission extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "admission_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID admissionUuid = UUID.randomUUID();

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "patient_name", length = 120)
    private String patientName;

    @Column(name = "hospital_name", length = 200)
    private String hospitalName;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "admission_reason", length = 1000)
    private String admissionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "ward_type", length = 20)
    @Builder.Default
    private WardType wardType = WardType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private IpdAdmissionStatus status = IpdAdmissionStatus.REQUESTED;

    @Column(name = "expected_admission_date")
    private LocalDate expectedAdmissionDate;

    @Column(name = "actual_admission_date")
    private LocalDate actualAdmissionDate;

    @Column(name = "discharge_date")
    private LocalDate dischargeDate;

    @Column(name = "bed_number", length = 20)
    private String bedNumber;

    @Column(name = "attending_doctor_name", length = 120)
    private String attendingDoctorName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;
}
