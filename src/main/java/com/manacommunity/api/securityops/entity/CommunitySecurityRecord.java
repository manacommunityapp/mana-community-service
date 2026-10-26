package com.manacommunity.api.securityops.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_security_record", indexes = {
        @Index(name = "idx_security_record_scope", columnList = "community_id,record_type,status"),
        @Index(name = "idx_security_record_resident", columnList = "resident_id,record_type")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommunitySecurityRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id")
    private AppUser resident;

    @Column(name = "record_type", nullable = false, length = 40)
    private String recordType;
    @Column(length = 150) private String name;
    @Column(length = 30) private String phone;
    @Column(name = "vehicle_number", length = 30) private String vehicleNumber;
    @Column(name = "valid_from", length = 40) private String validFrom;
    @Column(name = "valid_until", length = 40) private String validUntil;
    @Column(name = "time_window", length = 100) private String timeWindow;
    @Column(length = 500) private String purpose;
    @Column(length = 200) private String location;
    @Column(length = 30) @Builder.Default private String status = "ACTIVE";
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "otp_hash", length = 128) private String otpHash;
    @Column(name = "otp_expires_at") private LocalDateTime otpExpiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); if (createdAt == null) createdAt = now; if (updatedAt == null) updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
