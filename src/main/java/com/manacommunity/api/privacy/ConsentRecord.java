package com.manacommunity.api.privacy;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

/**
 * Persisted consent record per DPDP Act 2023, s.6 — one row per user per purpose.
 * Each grant/revoke creates a new immutable row; the latest row per (userId, purpose)
 * is authoritative. Never delete rows — they form the audit trail.
 */
@Entity
@Audited
@Table(name = "consent_records", indexes = {
    @Index(name = "idx_cr_user_purpose", columnList = "user_id,consent_purpose"),
    @Index(name = "idx_cr_user_id",      columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Consent purpose codes:
     * ESSENTIAL           – gate access, emergency, resident verification
     * MARKETING           – community offers, group-buying promotions
     * ANALYTICS           – anonymous in-app usage telemetry
     * THIRD_PARTY_SHARING – sharing contact with verified home-service vendors
     * BIOMETRIC           – biometric turnstile enrolment
     * PUSH_NOTIFICATIONS  – push + SMS notification delivery
     */
    @Column(name = "consent_purpose", nullable = false, length = 50)
    private String consentPurpose;

    @Column(name = "granted", nullable = false)
    private Boolean granted;

    /** Free-text IP or device token for evidentiary trail. */
    @Column(name = "source_ip", length = 50)
    private String sourceIp;

    /** UI context where consent was given: REGISTRATION, SETTINGS, BANNER. */
    @Column(name = "collection_point", length = 30)
    private String collectionPoint;

    /** Version of the privacy notice the user saw when consenting. */
    @Column(name = "notice_version", length = 20)
    private String noticeVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
