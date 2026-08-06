package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_branding")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityBranding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false, unique = true)
    private Community community;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "primary_color", nullable = false, length = 20)
    private String primaryColor;

    @Column(name = "secondary_color", length = 20)
    private String secondaryColor;

    @Column(name = "font_family", length = 120)
    private String fontFamily;

    @Column(name = "button_style", length = 40)
    private String buttonStyle;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "support_email", length = 160)
    private String supportEmail;

    @Column(name = "support_phone", length = 40)
    private String supportPhone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (primaryColor == null || primaryColor.isBlank()) {
            primaryColor = "#2563eb";
        }
        if (secondaryColor == null || secondaryColor.isBlank()) {
            secondaryColor = "#0f172a";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
