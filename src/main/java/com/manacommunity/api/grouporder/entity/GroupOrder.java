package com.manacommunity.api.grouporder.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "group_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupOrder {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "source_vendor", length = 200)
    private String sourceVendor;

    @Column(name = "item_name", nullable = false, length = 300)
    private String itemName;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "unit_description", length = 100)
    private String unitDescription;

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity = 5;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Column(name = "total_committed", nullable = false)
    private int totalCommitted = 0;

    @Column(name = "closes_at", nullable = false)
    private OffsetDateTime closesAt;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "delivery_point", length = 200)
    private String deliveryPoint;

    @Column(nullable = false, length = 30)
    private String status = "OPEN";

    @Column(name = "final_unit_price", precision = 10, scale = 2)
    private BigDecimal finalUnitPrice;

    @Column(name = "vendor_order_ref", length = 200)
    private String vendorOrderRef;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
