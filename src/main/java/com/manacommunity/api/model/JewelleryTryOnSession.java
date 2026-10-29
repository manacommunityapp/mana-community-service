package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jewellery_tryon_session", indexes = {
    @Index(name = "idx_jts_user_date", columnList = "user_id, started_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JewelleryTryOnSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private JewelleryProduct product;

    /** MIRRAR | BANUBA | CUSTOM */
    @Column(name = "provider", nullable = false, length = 40)
    private String provider;

    /** LIVE_AR | PHOTO */
    @Column(name = "mode", nullable = false, length = 20)
    private String mode;

    /** WEB | ANDROID | IOS */
    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    /** STARTED | ACTIVE | ENDED | FAILED */
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "STARTED";

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }
}
