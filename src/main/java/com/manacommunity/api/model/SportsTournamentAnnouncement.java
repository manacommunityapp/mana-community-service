package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A free-text announcement attached to a {@link SportsTournament}, shown in the
 * "Latest Announcements" section of the announcement email.
 */
@Entity
@Table(name = "sports_tournament_announcement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsTournamentAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonIgnoreProperties({"sportsEvents", "sponsors", "contacts"})
    private SportsTournament tournament;

    @Column(length = 150)
    private String title;

    @Column(length = 2000, nullable = false)
    private String content;

    @Column(length = 20)
    private String icon;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
