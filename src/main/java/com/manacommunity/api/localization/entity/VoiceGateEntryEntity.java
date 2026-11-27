package com.manacommunity.api.localization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voice_gate_entries", schema = "localization")
@Getter
@Setter
@NoArgsConstructor
public class VoiceGateEntryEntity {

    @Id
    @Column(name = "entry_id", nullable = false)
    private UUID entryId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "guard_user_id", nullable = false, length = 100)
    private String guardUserId;

    @Column(name = "flat_number", nullable = false, length = 40)
    private String flatNumber;

    @Column(name = "guard_locale", nullable = false, length = 15)
    private String guardLocale = "te";

    @Column(name = "resident_locale", nullable = false, length = 15)
    private String residentLocale = "en";

    @Column(name = "audio_url", length = 1000, nullable = false)
    private String audioUrl;

    @Column(name = "audio_codec", nullable = false, length = 30)
    private String audioCodec = "OPUS_16K";

    @Column(name = "duration_ms", nullable = false)
    private int durationMs = 2000;

    @Column(name = "audio_size_bytes", nullable = false)
    private int audioSizeBytes = 5120;

    @Column(name = "transcript_raw", columnDefinition = "TEXT")
    private String transcriptRaw;

    @Column(name = "transcript_translated", columnDefinition = "TEXT")
    private String transcriptTranslated;

    @Column(name = "detected_visitor_name", length = 150)
    private String detectedVisitorName;

    @Column(name = "detected_org_name", length = 150)
    private String detectedOrgName;

    @Column(name = "entity_confidence")
    private Double entityConfidence;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DISPATCHED_AUDIO";

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "transcribed_at")
    private Instant transcribedAt;
}
