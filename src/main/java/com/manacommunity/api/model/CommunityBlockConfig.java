package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores the block-level flat layout for a community (APARTMENT type).
 * One row per block (A, B, C, D). Flat lists are derived in-memory from
 * totalFloors and flatsPerFloor -- no individual flat rows are stored.
 */
@Entity
@Table(
    name = "community_block_config",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_community_block",
        columnNames = {"community_id", "block_name"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityBlockConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to the owning community. */
    @Column(name = "community_id", nullable = false)
    private Long communityId;

    /** Block label: A, B, C, D, etc. */
    @Column(name = "block_name", nullable = false, length = 10)
    private String blockName;

    /** Number of floors in this block (default 10). */
    @Column(name = "total_floors", nullable = false)
    @Builder.Default
    private int totalFloors = 10;

    /**
     * Number of flats per floor.
     * A/B/D = 11, C = 12 (as configured per community layout).
     */
    @Column(name = "flats_per_floor", nullable = false)
    private int flatsPerFloor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}