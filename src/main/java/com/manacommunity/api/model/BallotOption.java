package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ballot_option")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BallotOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ballot_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CommunityBallot ballot;

    @Column(nullable = false)
    private String text;

    @Column(columnDefinition = "TEXT")
    private String description;
}
