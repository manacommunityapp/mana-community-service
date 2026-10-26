package com.manacommunity.api.healthguard.entity;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.ResponderAvailability;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_responder", schema = "manacommunity")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CommunityResponder {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Builder.Default
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ResponderAvailability availabilityStatus = ResponderAvailability.AVAILABLE;

    @Column(columnDefinition = "text")
    private String skills;

    @Column(columnDefinition = "text")
    private String certifications;

    private String phoneNumber;

    @Builder.Default
    private Integer totalResponseCount = 0;

    private LocalDateTime lastPingAt;

    @Column(updatable = false)
    private LocalDateTime registeredAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastPingAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
