package com.manacommunity.api.notification.entity;

import com.manacommunity.api.notification.enums.OtpPurpose;
import com.manacommunity.api.notification.enums.OtpStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_otp_transaction", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsOtpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /** BCrypt hash of the OTP digits — raw OTP is never persisted */
    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OtpStatus status = OtpStatus.CREATED;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "attempt_count")
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "max_attempts")
    @Builder.Default
    private int maxAttempts = 5;

    /** IP address of the requester for rate-limit / audit */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /** FK to app_user — nullable for pre-registration flows */
    @Column(name = "user_id")
    private Long userId;

    /** FK to sms_message that delivered this OTP */
    @Column(name = "sms_message_id")
    private Long smsMessageId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
