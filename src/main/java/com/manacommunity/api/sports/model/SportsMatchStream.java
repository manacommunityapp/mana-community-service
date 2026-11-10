package com.manacommunity.api.sports.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sports_match_stream",
        indexes = @Index(name = "idx_sports_stream_match", columnList = "match_id", unique = true))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsMatchStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References SportsTournamentMatch.id — stored as plain FK column for cross-package simplicity */
    @Column(name = "match_id", nullable = false, unique = true)
    private Long matchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StreamStatus status = StreamStatus.OFFLINE;

    @Column(length = 300)
    private String title;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "viewer_peak")
    @Builder.Default
    private Integer viewerPeak = 0;

    @OneToMany(mappedBy = "stream", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt ASC")
    @Builder.Default
    private List<SportsMatchStreamCamera> cameras = new ArrayList<>();

    @OneToMany(mappedBy = "stream", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("postedAt ASC")
    @Builder.Default
    private List<SportsMatchStreamMessage> messages = new ArrayList<>();
}
