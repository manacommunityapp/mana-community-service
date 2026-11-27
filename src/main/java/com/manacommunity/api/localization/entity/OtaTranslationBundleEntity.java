package com.manacommunity.api.localization.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ota_translation_bundles", schema = "localization")
@Getter
@Setter
@NoArgsConstructor
public class OtaTranslationBundleEntity {

    @Id
    @Column(name = "bundle_id", nullable = false)
    private UUID bundleId;

    @Column(name = "locale", nullable = false, length = 15)
    private String locale;

    @Column(name = "namespace", nullable = false, length = 60)
    private String namespace;

    @Column(name = "version_hash", nullable = false, length = 64)
    private String versionHash;

    @Column(name = "direction", nullable = false, length = 3)
    private String direction = "ltr";

    @Column(name = "font_family", nullable = false, length = 80)
    private String fontFamily = "NotoSans";

    @Column(name = "line_height_multiplier", nullable = false)
    private double lineHeightMultiplier = 1.5;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strings_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> stringsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tts_hints_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> ttsHintsJson;

    @Column(name = "audio_pack_url", length = 1000)
    private String audioPackUrl;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt = Instant.now();
}
