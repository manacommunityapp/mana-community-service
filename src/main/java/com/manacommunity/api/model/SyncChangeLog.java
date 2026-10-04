package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_change_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(nullable = false, length = 10)
    private String action;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "server_vector_timestamp", nullable = false)
    private Long serverVectorTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
