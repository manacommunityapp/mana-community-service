package com.manacommunity.api.access.biometric;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "turnstile_apb_states",
    uniqueConstraints = @UniqueConstraint(name = "uq_turnstile_apb_cred", columnNames = {"community_id", "zone_id", "credential_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnstileApbState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "zone_id", nullable = false, length = 64)
    private String zoneId;

    @Column(name = "credential_id", nullable = false, length = 128)
    private String credentialId;

    @Builder.Default
    @Column(name = "current_state", nullable = false, length = 16)
    private String currentState = "OUTSIDE"; // INSIDE, OUTSIDE

    @Builder.Default
    @Column(name = "last_direction", nullable = false, length = 16)
    private String lastDirection = "EXIT"; // ENTRY, EXIT

    @Column(name = "last_turnstile_id", length = 64)
    private String lastTurnstileId;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Column(name = "exited_at")
    private LocalDateTime exitedAt;

    @Builder.Default
    @Column(name = "last_event_at", nullable = false)
    private LocalDateTime lastEventAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "soft_override_count", nullable = false)
    private int softOverrideCount = 0;
}
