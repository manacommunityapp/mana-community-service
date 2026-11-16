package com.manacommunity.api.wellnessbasket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 4-week rotating daily menu for the Mana Fresh Wellness subscription.
 * One row = one day in the cycle for one plan type.
 * Unique constraint: (plan_type, cycle_week, day_of_week).
 */
@Getter
@Setter
@Entity
@Table(
    name = "wellness_menu_cycle",
    schema = "manacommunity",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_wmc_plan_week_day",
        columnNames = {"plan_type", "cycle_week", "day_of_week"}
    )
)
public class WellnessMenuCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** BASIC | PREMIUM */
    @Column(name = "plan_type", nullable = false, length = 30)
    private String planType;

    /** 1–4 */
    @Column(name = "cycle_week", nullable = false)
    private Short cycleWeek;

    /** 1=Mon, 2=Tue, …, 6=Sat */
    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    /** Ordered list of fresh_food_product_config IDs */
    @Column(name = "product_ids", columnDefinition = "bigint[]", nullable = false)
    private Long[] productIds = new Long[0];

    /** Alternate product IDs for allergy substitution */
    @Column(name = "alt_product_ids", columnDefinition = "bigint[]")
    private Long[] altProductIds = new Long[0];

    @Column(name = "dietitian_notes", columnDefinition = "text")
    private String dietitianNotes;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    /** Date from which this menu cycle is in effect */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
}
