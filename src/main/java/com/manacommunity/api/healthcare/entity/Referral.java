package com.manacommunity.api.healthcare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "referral")
@Data
public class Referral {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "referring_doctor_id", nullable = false) private Long referringDoctorId;
    @Column(name = "patient_user_id", nullable = false) private Long patientUserId;
    @Column(name = "referred_to_name", nullable = false, length = 300) private String referredToName;
    @Column(name = "referred_to_type", nullable = false, length = 100) private String referredToType;
    @Column(length = 200) private String specialization;
    @Column(nullable = false, length = 1000) private String reason;
    @Column(length = 50) private String urgency = "ROUTINE";
    @Column(name = "referral_date") private LocalDate referralDate = LocalDate.now();
    @Column(name = "appointment_booked") private Boolean appointmentBooked = false;
    @Column(name = "appointment_date") private LocalDate appointmentDate;
    @Column(name = "outcome_notes", length = 1000) private String outcomeNotes;
    @Column(name = "feedback_to_referrer", length = 1000) private String feedbackToReferrer;
    @Column(length = 50) private String status = "PENDING";
    @Column(name = "created_at") private Instant createdAt = Instant.now();
    @Column(name = "updated_at") private Instant updatedAt = Instant.now();
}
