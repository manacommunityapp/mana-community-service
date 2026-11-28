package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "merchandise_order")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchandiseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "merchandise_id", nullable = false) private Long merchandiseId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Builder.Default @Column(nullable = false) private Integer quantity = 1;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;
    @Column(name = "payment_ref", length = 100) private String paymentRef;
    @Builder.Default @Column(name = "created_at") private Instant createdAt = Instant.now();
}
