package com.manacommunity.api.healthcare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "prescription_followup")
@Data
public class PrescriptionFollowup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "appointment_id", nullable = false) private Long appointmentId;
    @Column(name = "patient_user_id", nullable = false) private Long patientUserId;
    @Column(name = "doctor_id", nullable = false) private Long doctorId;
    @Column(name = "medication_name", nullable = false, length = 500) private String medicationName;
    @Column(length = 200) private String dosage;
    @Column(name = "duration_days") private Integer durationDays;
    @Column(name = "followup_due_date", nullable = false) private LocalDate followupDueDate;
    @Column(name = "followup_note", length = 1000) private String followupNote;
    @Column(name = "reminder_sent") private Boolean reminderSent = false;
    @Column(name = "reminder_sent_at") private Instant reminderSentAt;
    @Column private Boolean completed = false;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
