package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "committee_elections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommitteeElection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "position_id", nullable = false)
    private Long positionId;

    @Column(name = "nomination_start", nullable = false)
    private OffsetDateTime nominationStart;

    @Column(name = "nomination_end", nullable = false)
    private OffsetDateTime nominationEnd;

    @Column(name = "voting_start", nullable = false)
    private OffsetDateTime votingStart;

    @Column(name = "voting_end", nullable = false)
    private OffsetDateTime votingEnd;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "NOMINATIONS_OPEN";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
