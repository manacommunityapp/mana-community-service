package com.manacommunity.api.healthcare.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "caregiver_consent")
@Data
public class CaregiverConsent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "patient_user_id", nullable = false) private Long patientUserId;
    @Column(name = "caregiver_user_id", nullable = false) private Long caregiverUserId;
    @Column(name = "caregiver_name", nullable = false, length = 300) private String caregiverName;
    @Column(nullable = false, length = 100) private String relationship;
    @Column(name = "consent_scope", nullable = false, length = 500) private String consentScope;
    @Column(name = "valid_from", nullable = false) private LocalDate validFrom;
    @Column(name = "valid_until") private LocalDate validUntil;
    @Column private Boolean revoked = false;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "revoked_by") private Long revokedBy;
    @Column(name = "document_url", length = 1000) private String documentUrl;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
    @Column(name = "updated_at") private Instant updatedAt = Instant.now();
}
