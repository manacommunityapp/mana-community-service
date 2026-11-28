package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity @Table(name = "hs_recurring_plan") @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HsRecurringPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "package_id") private Long packageId;
    @Column(nullable = false, columnDefinition = "hs_service_category") private String category;
    @Column(nullable = false, columnDefinition = "hs_plan_frequency") private String frequency;
    @Column(name = "next_service_date", nullable = false) private LocalDate nextServiceDate;
    @Column(name = "preferred_time") private LocalTime preferredTime;
    @Column(name = "sku_code", length = 64) private String skuCode;
    @Column(name = "vendor_id") private Long vendorId;
    @Column(name = "tower_block", length = 32) private String towerBlock;
    @Builder.Default @Column(name = "floor_number") private Integer floorNumber = 0;
    @Column(name = "flat_number", length = 32) private String flatNumber;
    @Builder.Default @Column(name = "default_qty", nullable = false) private Integer defaultQty = 1;
    @Builder.Default @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) private java.math.BigDecimal unitPrice = java.math.BigDecimal.ZERO;
    @Column(name = "vacation_from") private LocalDate vacationFrom;
    @Column(name = "vacation_until") private LocalDate vacationUntil;
    @Builder.Default @Column(nullable = false, columnDefinition = "hs_plan_status") private String status = "ACTIVE";
    @Builder.Default @Column(name = "created_at") private Instant createdAt = Instant.now();
    @Builder.Default @Column(name = "updated_at") private Instant updatedAt = Instant.now();
}
