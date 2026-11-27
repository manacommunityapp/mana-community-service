package com.manacommunity.api.finops.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "resident_channel_profiles", schema = "finops")
@Getter
@Setter
@NoArgsConstructor
public class ResidentChannelProfileEntity {

    @Id
    @Column(name = "resident_id", nullable = false, length = 100)
    private String residentId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "ws_last_seen_at")
    private Instant wsLastSeenAt;

    @Column(name = "fcm_p50_ms", nullable = false)
    private int fcmP50Ms = 1800;

    @Column(name = "fcm_p90_ms", nullable = false)
    private int fcmP90Ms = 3200;

    @Column(name = "fcm_ack_rate_7d", nullable = false)
    private double fcmAckRate7d = 0.85;

    @Column(name = "wa_p50_ms", nullable = false)
    private int waP50Ms = 4200;

    @Column(name = "wa_ack_rate_7d", nullable = false)
    private double waAckRate7d = 0.80;

    @Column(name = "wa_opt_in", nullable = false)
    private boolean waOptIn = true;

    @Column(name = "ivr_ack_rate_7d", nullable = false)
    private double ivrAckRate7d = 0.92;

    @Column(name = "oem_aggressive_doze", nullable = false)
    private boolean oemAggressiveDoze = false;

    @Column(name = "preferred_channel", nullable = false, length = 30)
    private String preferredChannel = "FCM_PUSH";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
