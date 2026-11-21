package com.manacommunity.api.devices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "device_visitor_otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceVisitorOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "issued_by", nullable = false)
    private Long issuedBy;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "visitor_name", length = 200)
    private String visitorName;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom = Instant.now();

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(nullable = false)
    private boolean revoked = false;
}
