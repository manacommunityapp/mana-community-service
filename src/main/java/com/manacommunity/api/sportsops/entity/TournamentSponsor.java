package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tournament_sponsor")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TournamentSponsor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "tournament_id", nullable = false) private Long tournamentId;
    @Column(name = "sponsor_name", nullable = false, length = 255) private String sponsorName;
    @Column(length = 20) private String tier = "BRONZE";
    @Column(precision = 12, scale = 2) private BigDecimal amount = BigDecimal.ZERO;
    @Column(name = "logo_key", length = 500) private String logoKey;
    @Column(columnDefinition = "TEXT") private String benefits;
    @Column(length = 20) private String status = "PENDING";
    @Column(name = "contact_name", length = 255) private String contactName;
    @Column(name = "contact_email", length = 255) private String contactEmail;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
