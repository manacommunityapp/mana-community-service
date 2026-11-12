package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ballot_vote", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ballot_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BallotVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ballot_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommunityBallot ballot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private BallotOption option;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "receipt_hash", nullable = false)
    private String receiptHash;

    @Column(name = "voted_at")
    @Builder.Default
    private LocalDateTime votedAt = LocalDateTime.now();
}
