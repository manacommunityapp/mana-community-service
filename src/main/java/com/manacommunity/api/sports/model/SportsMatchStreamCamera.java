package com.manacommunity.api.sports.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_match_stream_camera")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsMatchStreamCamera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_id", nullable = false)
    private SportsMatchStream stream;

    @Column(nullable = false, length = 100)
    private String label;

    /** Raw URL as entered by the organiser (YouTube, Twitch, or HTTPS) */
    @Column(nullable = false, length = 2048)
    private String url;

    /** Client-side resolved embed URL — stored so mobile can embed directly */
    @Column(name = "embed_url", nullable = false, length = 2048)
    private String embedUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StreamPlatform platform;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = false;

    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
