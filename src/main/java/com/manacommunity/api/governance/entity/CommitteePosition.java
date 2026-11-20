package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "committee_positions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommitteePosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "max_holders", nullable = false)
    @Builder.Default
    private short maxHolders = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
