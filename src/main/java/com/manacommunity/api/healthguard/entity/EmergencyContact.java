package com.manacommunity.api.healthguard.entity;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.ContactRelationship;
import com.manacommunity.api.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_contact", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 255)
    private String name;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 255)
    private String phone;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ContactRelationship relationship = ContactRelationship.OTHER;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 1;

    @Builder.Default
    private Boolean notifyOnSos = true;

    @Builder.Default
    private Boolean notifyOnAlert = true;

    @Builder.Default
    private Boolean consentGiven = false;

    private LocalDateTime consentGivenAt;

    @Builder.Default
    private Boolean isActive = true;

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
