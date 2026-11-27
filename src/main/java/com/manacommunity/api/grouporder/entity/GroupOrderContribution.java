package com.manacommunity.api.grouporder.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "group_order_contributions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_order_id", "resident_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupOrderContribution {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_order_id", nullable = false)
    private Long groupOrderId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "unit_price_at_join", precision = 10, scale = 2)
    private BigDecimal unitPriceAtJoin;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus = "PENDING";

    @Column(name = "payment_ref", length = 200)
    private String paymentRef;

    @Column(name = "collection_status", nullable = false, length = 30)
    private String collectionStatus = "PENDING";

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt = OffsetDateTime.now();
}
