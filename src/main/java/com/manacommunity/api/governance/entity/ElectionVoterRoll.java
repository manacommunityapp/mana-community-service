package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "election_voter_roll",
    uniqueConstraints = @UniqueConstraint(name = "uq_election_voter_roll_flat", columnNames = {"election_id", "flat_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ElectionVoterRoll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "blinded_token_hash", nullable = false, length = 64)
    private String blindedTokenHash;

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private OffsetDateTime issuedAt;
}
