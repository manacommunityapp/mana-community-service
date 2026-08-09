package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_department")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "head_name", length = 120)
    private String headName;

    @Builder.Default
    @Column(name = "total_target", nullable = false)
    private Integer totalTarget = 10;

    @Builder.Default
    @Column(name = "present_count", nullable = false)
    private Integer presentCount = 0;

    @Builder.Default
    @Column(length = 30)
    private String color = "#6366f1";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

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
