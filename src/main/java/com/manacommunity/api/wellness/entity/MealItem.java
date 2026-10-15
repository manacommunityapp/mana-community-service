package com.manacommunity.api.wellness.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "meal_item", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_log_id", nullable = false)
    private MealLog mealLog;

    @Column(name = "food_name", nullable = false)
    private String foodName;

    @Column(name = "food_name_local")
    private String foodNameLocal;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "calories")
    private Integer calories;

    @Column(name = "protein")
    private BigDecimal protein;

    @Column(name = "carbs")
    private BigDecimal carbs;

    @Column(name = "fat")
    private BigDecimal fat;

    @Column(name = "fiber")
    private BigDecimal fiber;

    @Column(name = "food_catalog_id")
    private Long foodCatalogId;
}
