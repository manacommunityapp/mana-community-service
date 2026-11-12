package com.manacommunity.api.fresh.catalog.model;

import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fresh_product_nutrition", schema = "manacommunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FreshProductNutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id", nullable = false, unique = true)
    private VendorProduct vendorProduct;

    @Column(name = "serving_size_grams", precision = 8, scale = 2)
    private BigDecimal servingSizeGrams;

    @Column(name = "calories_kcal", precision = 8, scale = 2)
    private BigDecimal caloriesKcal;

    @Column(name = "protein_g", precision = 8, scale = 2)
    private BigDecimal proteinG;

    @Column(name = "carbohydrates_g", precision = 8, scale = 2)
    private BigDecimal carbohydratesG;

    @Column(name = "fat_g", precision = 8, scale = 2)
    private BigDecimal fatG;

    @Column(name = "saturated_fat_g", precision = 8, scale = 2)
    private BigDecimal saturatedFatG;

    @Column(name = "fibre_g", precision = 8, scale = 2)
    private BigDecimal fibreG;

    @Column(name = "sodium_mg", precision = 8, scale = 2)
    private BigDecimal sodiumMg;

    @Column(name = "sugar_g", precision = 8, scale = 2)
    private BigDecimal sugarG;

    @Column(name = "micronutrients", columnDefinition = "JSONB")
    private String micronutrients;

    @Column(name = "dietitian_certified", nullable = false)
    @Builder.Default
    private Boolean dietitianCertified = false;

    @Column(name = "dietitian_name", length = 120)
    private String dietitianName;

    @Column(name = "certification_number", length = 60)
    private String certificationNumber;

    @Column(name = "certified_on")
    private LocalDate certifiedOn;

    @Column(name = "fssai_label_compliant", nullable = false)
    @Builder.Default
    private Boolean fssaiLabelCompliant = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }
}