package com.manacommunity.api.communityops.pass;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "digital_pass")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_type", nullable = false)
    private PassType passType;

    @Column(name = "visitor_name")
    private String visitorName;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    private String purpose;

    @Column(nullable = false, unique = true)
    private String token;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassStatus status = PassStatus.ACTIVE;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_by_guard")
    private Long usedByGuard;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
