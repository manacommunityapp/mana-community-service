package com.manacommunity.api.localization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tts_audio_cache", schema = "localization")
@Getter
@Setter
@NoArgsConstructor
public class TtsAudioCacheEntity {

    @Id
    @Column(name = "cache_key", nullable = false, length = 64)
    private String cacheKey;

    @Column(name = "locale", nullable = false, length = 15)
    private String locale;

    @Column(name = "voice_id", nullable = false, length = 80)
    private String voiceId;

    @Column(name = "normalized_text", columnDefinition = "TEXT", nullable = false)
    private String normalizedText;

    @Column(name = "audio_url", length = 1000, nullable = false)
    private String audioUrl;

    @Column(name = "codec", nullable = false, length = 20)
    private String codec = "OPUS_16K";

    @Column(name = "duration_ms", nullable = false)
    private int durationMs = 1500;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes = 5120;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
