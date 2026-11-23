package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_showcase_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformShowcaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_key", nullable = false, unique = true, length = 50)
    private String itemKey;

    @Column(nullable = false, length = 16)
    private String emoji;

    @Column(name = "accent_color", nullable = false, length = 20)
    private String accentColor;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 120)
    private String tagline;

    @Column(nullable = false, length = 320)
    private String description;

    @Column(name = "tags_csv", nullable = false, length = 255)
    private String tagsCsv;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }
}
