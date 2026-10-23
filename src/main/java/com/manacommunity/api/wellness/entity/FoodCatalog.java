package com.manacommunity.api.wellness.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "food_catalog", schema = "manacommunity")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodCatalog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_local")
    private String nameLocal;

    @Column(name = "category")
    private String category;

    @Column(name = "cuisine")
    private String cuisine;

    @Column(name = "serving_size")
    private String servingSize;

    @Column(name = "serving_unit")
    private String servingUnit;

    @Column(name = "calories_per_serving")
    private Integer caloriesPerServing;

    @Column(name = "protein_per_serving")
    private BigDecimal proteinPerServing;

    @Column(name = "carbs_per_serving")
    private BigDecimal carbsPerServing;

    @Column(name = "fat_per_serving")
    private BigDecimal fatPerServing;

    @Column(name = "fiber_per_serving")
    private BigDecimal fiberPerServing;

    @Column(name = "is_vegetarian")
    private Boolean isVegetarian;

    @Column(name = "is_vegan")
    private Boolean isVegan;

    @Column(name = "region")
    private String region;

    @Column(name = "verified")
    private Boolean verified;
}
