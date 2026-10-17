package com.manacommunity.api.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Append-only consent ledger. The latest event per (user, purpose, partner) wins. */
@Entity
@Table(name = "insurance_consent_record")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ConsentRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InsuranceEnums.ConsentPurpose purpose;
    @Column(name = "partner_id")
    private Long partnerId;
    @Column(name = "notice_version", nullable = false, length = 20)
    private String noticeVersion;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 10)
    private InsuranceEnums.ConsentEvent eventType;
    @Column(name = "granted_ref_id")
    private Long grantedRefId;
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
