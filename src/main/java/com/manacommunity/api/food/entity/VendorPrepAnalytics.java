package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "vendor_prep_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorPrepAnalytics {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long vendorId;
    private Long communityId;
    private Long orderId;
    private String orderType;
    private Instant orderPlacedAt;
    private Instant prepStartedAt;
    private Instant prepCompletedAt;
    private Long slotId;
    private Boolean wasOnTime;
    private Instant recordedAt;
}
