package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "proposal_support", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"proposal_id", "user_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommunityProposal proposal;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "supported_at")
    @Builder.Default
    private LocalDateTime supportedAt = LocalDateTime.now();
}
