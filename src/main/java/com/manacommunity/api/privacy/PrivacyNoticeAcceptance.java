package com.manacommunity.api.privacy;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "privacy_notice_acceptances", indexes = {
    @Index(name = "idx_pna_resident",        columnList = "resident_id"),
    @Index(name = "idx_pna_community",       columnList = "community_id"),
    @Index(name = "idx_pna_resident_version", columnList = "resident_id,notice_version", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyNoticeAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "notice_version", nullable = false, length = 20)
    private String noticeVersion;

    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (acceptedAt == null) acceptedAt = now;
        if (createdAt == null)  createdAt  = now;
    }
}
