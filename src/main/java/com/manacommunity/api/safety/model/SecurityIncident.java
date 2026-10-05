package com.manacommunity.api.safety.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_incident", indexes = {
        @Index(name = "idx_security_incident_community", columnList = "community_id"),
        @Index(name = "idx_security_incident_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "OPEN";

    @Column(length = 30)
    private String priority;

    @Column(length = 500)
    private String location;

    @Column(name = "reported_by_id")
    private Long reportedById;

    @Column(name = "reported_by_name")
    private String reportedByName;

    @Column(name = "assigned_to_id")
    private Long assignedToId;

    @Column(name = "assigned_to_name")
    private String assignedToName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "resolution_notes", length = 2000)
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
