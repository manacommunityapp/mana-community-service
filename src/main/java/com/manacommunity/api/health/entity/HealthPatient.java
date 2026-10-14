package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.FamilyRelationship;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "health_patient", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthPatient extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "patient_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID patientUuid = UUID.randomUUID();

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private FamilyRelationship relationship = FamilyRelationship.SELF;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "emergency_contact_phone", length = 50)
    private String emergencyContactPhone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
