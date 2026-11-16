package com.manacommunity.api.wellnessbasket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Nutritional data for a product in the Mana Fresh Wellness catalogue.
 * Linked 1-to-1 with {@code fresh_food_product_config}.
 */
@Getter
@Setter
@Entity
@Table(name = "wellness_product_nutrition", schema = "manacommunity")
public class WellnessProductNutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to fresh_food_product_config.id */
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "energy_kcal", nullable = false, precision = 8, scale = 2)
    private BigDecimal energyKcal;

    @Column(name = "protein_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal carbsG;

    @Column(name = "sugar_g", precision = 8, scale = 2)
    private BigDecimal sugarG;

    @Column(name = "fat_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal fatG;

    @Column(name = "saturated_fat_g", precision = 8, scale = 2)
    private BigDecimal saturatedFatG;

    @Column(name = "fibre_g", precision = 8, scale = 2)
    private BigDecimal fibreG;

    @Column(name = "sodium_mg", precision = 8, scale = 2)
    private BigDecimal sodiumMg;

    @Column(name = "glycemic_index")
    private Integer glycemicIndex;

    @Column(name = "per_serving_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal perServingG = BigDecimal.valueOf(100);

    /**
     * PostgreSQL TEXT[] — stored as a String array.
     * Values: WEIGHT_LOSS, DIABETIC_FRIENDLY, HEART_HEALTH, etc.
     */
    @Column(name = "goal_tags", columnDefinition = "text[]")
    private String[] goalTags = new String[0];

    /**
     * PostgreSQL TEXT[] — allergen labels per FSSAI.
     * Values: GLUTEN, DAIRY, NUTS, SOY, EGG, etc.
     */
    @Column(name = "allergen_flags", columnDefinition = "text[]")
    private String[] allergenFlags = new String[0];

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
