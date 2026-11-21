package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "election_votes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"election_id", "voter_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ElectionVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "voter_id", nullable = false)
    private Long voterId;

    @Column(name = "nomination_id", nullable = false)
    private Long nominationId;

    @CreationTimestamp
    @Column(name = "voted_at", updatable = false)
    private OffsetDateTime votedAt;
}
