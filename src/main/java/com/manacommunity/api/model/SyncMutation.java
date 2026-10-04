package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_mutations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncMutation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mutation_id", nullable = false, unique = true, length = 50)
    private String mutationId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(nullable = false, length = 10)
    private String action;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "vector_timestamp", nullable = false)
    private Long vectorTimestamp;

    @Column(nullable = false, length = 15)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "PENDING";
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
