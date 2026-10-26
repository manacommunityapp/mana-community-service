package com.manacommunity.api.health.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_clinic", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthClinic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "clinic_name", nullable = false, length = 255)
    private String clinicName;

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(length = 100)
    @Builder.Default
    private String city = "Bengaluru";

    @Column(length = 20)
    private String pincode;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "primary_phone", nullable = false, length = 50)
    private String primaryPhone;

    @Column(name = "emergency_phone", length = 50)
    private String emergencyPhone;

    @Column(name = "is_within_community_premises", nullable = false)
    @Builder.Default
    private Boolean isWithinCommunityPremises = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
