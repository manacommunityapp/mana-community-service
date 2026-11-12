package com.manacommunity.api.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sports_court")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "venue")
@EqualsAndHashCode(exclude = "venue")
public class SportsCourt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String color;

    @Column(name = "opening_time", length = 20)
    private String openingTime;

    @Column(name = "closing_time", length = 20)
    private String closingTime;

    @Column(name = "sport_type", length = 50)
    private String sportType;

    @Column(name = "surface_type", length = 50)
    private String surfaceType;

    @Column(name = "is_indoor")
    @Builder.Default
    private Boolean isIndoor = false;

    @Column(name = "has_lighting")
    @Builder.Default
    private Boolean hasLighting = true;

    @Column(name = "base_hourly_price", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal baseHourlyPrice = new java.math.BigDecimal("400.00");

    @Column(name = "resident_discount_percent", precision = 5, scale = 2)
    @Builder.Default
    private java.math.BigDecimal residentDiscountPercent = new java.math.BigDecimal("10.00");

    @Column(name = "pricing_rules", columnDefinition = "TEXT")
    private String pricingRules;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    @JsonIgnore
    private SportsVenue venue;
}
