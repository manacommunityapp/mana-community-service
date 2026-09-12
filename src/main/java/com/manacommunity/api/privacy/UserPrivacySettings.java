package com.manacommunity.api.privacy;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_privacy_settings", indexes = {
    @Index(name = "idx_ups_user", columnList = "user_id", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrivacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "show_phone_to_neighbours", nullable = false)
    @Builder.Default
    private Boolean showPhoneToNeighbours = false;

    @Column(name = "show_email_to_neighbours", nullable = false)
    @Builder.Default
    private Boolean showEmailToNeighbours = false;

    @Column(name = "show_flat_in_directory", nullable = false)
    @Builder.Default
    private Boolean showFlatInDirectory = true;

    @Column(name = "show_family_members", nullable = false)
    @Builder.Default
    private Boolean showFamilyMembers = false;

    @Column(name = "show_vehicle_in_directory", nullable = false)
    @Builder.Default
    private Boolean showVehicleInDirectory = false;

    @Column(name = "emergency_contact_restricted", nullable = false)
    @Builder.Default
    private Boolean emergencyContactRestricted = true;

    @Column(name = "allow_marketplace_contact", nullable = false)
    @Builder.Default
    private Boolean allowMarketplaceContact = true;

    @Column(name = "allow_event_tagging", nullable = false)
    @Builder.Default
    private Boolean allowEventTagging = true;

    @Column(name = "activity_visibility", nullable = false, length = 30)
    @Builder.Default
    private String activityVisibility = "COMMUNITY"; // PRIVATE, COMMUNITY, PUBLIC

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
