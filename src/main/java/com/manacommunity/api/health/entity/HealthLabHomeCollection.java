package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.LabCollectionStatus;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "health_lab_home_collection", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthLabHomeCollection extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "booking_uuid", nullable = false, unique = true)
    @Builder.Default
    private UUID bookingUuid = UUID.randomUUID();

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "package_id", length = 80)
    private String packageId;

    @Column(name = "package_name", length = 200)
    private String packageName;

    @Column(name = "collection_date", length = 20)
    private String collectionDate;

    @Column(name = "collection_slot", length = 50)
    private String collectionSlot;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(name = "phlebotomist_name", length = 150)
    private String phlebotomistName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private LabCollectionStatus status = LabCollectionStatus.BOOKED;

    @Column(name = "report_url", columnDefinition = "TEXT")
    private String reportUrl;
}
