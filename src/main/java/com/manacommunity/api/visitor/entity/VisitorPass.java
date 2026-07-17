package com.manacommunity.api.visitor.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_pass")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pass_code", nullable = false, unique = true, length = 12)
    private String passCode;

    @Column(name = "visitor_name", nullable = false, length = 100)
    private String visitorName;

    @Column(name = "visitor_phone", length = 15)
    private String visitorPhone;

    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_type", nullable = false, length = 20)
    @Builder.Default
    private PassType passType = PassType.PRE_APPROVED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PassStatus status = PassStatus.PENDING;

    @Column(name = "expected_at")
    private LocalDateTime expectedAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "flat_number", length = 30)
    private String flatNumber;

    @Column(name = "otp", length = 6)
    private String otp;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "gate_in", length = 100)
    private String gateIn;

    @Column(name = "gate_out", length = 100)
    private String gateOut;

    @Column(name = "guard_in", length = 100)
    private String guardIn;

    @Column(name = "guard_out", length = 100)
    private String guardOut;

    @Column(name = "visitor_photo", columnDefinition = "TEXT")
    private String visitorPhoto;

    @Column(name = "encrypted_token", length = 500)
    private String encryptedToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private AppUser resident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PassStatus.PENDING;
        if (passType == null) passType = PassType.PRE_APPROVED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum PassType { GUEST, DELIVERY, MAID, VENDOR, FAMILY, RECURRING, PRE_APPROVED, WALK_IN, OTHER }
    public enum PassStatus { PENDING, APPROVED, CHECKED_IN, CHECKED_OUT, REJECTED, EXPIRED }
}
