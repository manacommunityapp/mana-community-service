package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.RecordType;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "health_medical_record", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthMedicalRecord extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 50)
    private RecordType recordType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "document_file_url", length = 1000)
    private String documentFileUrl;
}
