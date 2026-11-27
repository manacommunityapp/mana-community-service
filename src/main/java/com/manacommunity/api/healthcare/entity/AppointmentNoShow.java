package com.manacommunity.api.healthcare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "appointment_no_show")
@Data
public class AppointmentNoShow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "appointment_id", nullable = false) private Long appointmentId;
    @Column(name = "doctor_id", nullable = false) private Long doctorId;
    @Column(name = "patient_user_id", nullable = false) private Long patientUserId;
    @Column(name = "no_show_at") private Instant noShowAt;
    @Column(name = "fee_charged", precision = 12, scale = 2) private BigDecimal feeCharged;
    @Column(name = "fee_waived") private Boolean feeWaived = false;
    @Column(name = "waiver_reason", length = 500) private String waiverReason;
    @Column(name = "reviewed_by") private Long reviewedBy;
    @Column(length = 1000) private String notes;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
