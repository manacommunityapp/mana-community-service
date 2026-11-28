package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_public_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPublicConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 40)
    @Builder.Default
    private String configKey = "default";

    @Column(name = "top_badge_text", nullable = false, length = 80)
    private String topBadgeText;

    @Column(name = "hero_capsule_text", nullable = false, length = 80)
    private String heroCapsuleText;

    @Column(name = "hero_title", nullable = false, length = 80)
    private String heroTitle;

    @Column(name = "hero_subtitle", nullable = false, length = 255)
    private String heroSubtitle;

    @Column(name = "security_label", nullable = false, length = 80)
    private String securityLabel;

    @Column(name = "certification_note", nullable = false, length = 160)
    private String certificationNote;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }
}
