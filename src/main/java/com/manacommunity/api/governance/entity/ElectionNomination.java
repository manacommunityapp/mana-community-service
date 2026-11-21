package com.manacommunity.api.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "election_nominations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"election_id", "nominee_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ElectionNomination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "election_id", nullable = false)
    private Long electionId;

    @Column(name = "nominee_id", nullable = false)
    private Long nomineeId;

    @Column(columnDefinition = "TEXT")
    private String manifesto;

    @CreationTimestamp
    @Column(name = "nominated_at", updatable = false)
    private OffsetDateTime nominatedAt;
}
