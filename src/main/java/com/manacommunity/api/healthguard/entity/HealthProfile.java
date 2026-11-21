package com.manacommunity.api.healthguard.entity;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.BloodGroup;
import com.manacommunity.api.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_profile", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private BloodGroup bloodGroup = BloodGroup.UNKNOWN;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String allergies;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String medicalConditions;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String emergencyNotes;

    private Integer heightCm;
    private Integer weightKg;
    private LocalDate dateOfBirth;

    @Builder.Default
    private Boolean organDonorConsent = false;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 255)
    private String medicalIdNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 255)
    private String physicianName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 255)
    private String physicianPhone;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
