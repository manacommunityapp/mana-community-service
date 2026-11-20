package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tournament_merchandise")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TournamentMerchandise {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "tournament_id", nullable = false) private Long tournamentId;
    @Column(name = "item_name", nullable = false, length = 255) private String itemName;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(name = "stock_qty") private Integer stockQty = 0;
    @Column(name = "image_key", length = 500) private String imageKey;
    @Column(name = "is_available") private Boolean isAvailable = true;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
