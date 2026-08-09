package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_venue_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventVenueConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "venue_name", length = 160)
    private String venueName;

    @Column(name = "zones_json", columnDefinition = "TEXT")
    private String zonesJson;

    @Column(name = "facilities_json", columnDefinition = "TEXT")
    private String facilitiesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
