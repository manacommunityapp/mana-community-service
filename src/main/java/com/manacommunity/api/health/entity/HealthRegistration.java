package com.manacommunity.api.health.entity;

import com.manacommunity.api.health.enums.HealthEnums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_registration", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;

    @Column(name = "council_name", nullable = false, length = 255)
    private String councilName;

    @Column(name = "issue_year")
    private Integer issueYear;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private VerificationStatus status = VerificationStatus.VERIFIED;

    @Column(name = "verification_doc_url", length = 1000)
    private String verificationDocUrl;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
