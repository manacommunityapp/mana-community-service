package com.manacommunity.api.groupbuying.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "group_buy_demand_offer")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityDemandOffer extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", nullable = false)
    @JsonIgnore
    private CommunityDemand demand;

    @Column(name = "vendor_id", length = 60)
    private String vendorId;

    @Column(name = "vendor_name", nullable = false, length = 120)
    private String vendorName;

    @Column(name = "vendor_rating")
    private Double vendorRating;

    @Column(name = "vendor_verified")
    private Boolean vendorVerified;

    @Column(name = "offered_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal offeredPrice;

    @Column(name = "minimum_qty", nullable = false)
    private Integer minimumQty;

    @Column(name = "maximum_qty")
    private Integer maximumQty;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "is_best_value")
    private Boolean isBestValue;

    @Column(columnDefinition = "TEXT")
    private String terms;
}
