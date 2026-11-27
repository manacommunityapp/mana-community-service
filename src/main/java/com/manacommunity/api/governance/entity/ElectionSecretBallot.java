package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "election_secret_ballots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ElectionSecretBallot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "nomination_id", nullable = false)
    private Long nominationId;

    @Column(name = "ballot_nullifier", nullable = false, unique = true, length = 64)
    private String ballotNullifier;

    @Column(name = "blind_signature", nullable = false, columnDefinition = "TEXT")
    private String blindSignature;

    @Column(name = "prev_ballot_hash", nullable = false, length = 64)
    private String prevBallotHash;

    @Column(name = "ballot_hash", nullable = false, length = 64)
    private String ballotHash;

    @Column(name = "receipt_code", nullable = false, unique = true, length = 32)
    private String receiptCode;

    @CreationTimestamp
    @Column(name = "cast_at", updatable = false)
    private OffsetDateTime castAt;
}
