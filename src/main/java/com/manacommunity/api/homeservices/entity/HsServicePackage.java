package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "hs_service_package") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsServicePackage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(nullable = false) private String name;
    @Column(nullable = false, columnDefinition = "hs_service_category") private String category;
    private String description;
    @Column(name = "duration_minutes", nullable = false) private Integer durationMinutes = 60;
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2) private BigDecimal basePrice;
    @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
