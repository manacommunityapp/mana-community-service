package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "generator_fuel_refills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratorFuelRefill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "generator_id", nullable = false)
    private GeneratorAsset generator;

    @Column(name = "refill_date", nullable = false)
    private LocalDate refillDate;

    @Column(name = "quantity_liters", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityLiters;

    @Column(name = "price_per_liter", nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerLiter;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "supplier_name", length = 150)
    private String supplierName;

    @Column(name = "bill_number", length = 80)
    private String billNumber;

    @Column(name = "entered_by", length = 120)
    private String enteredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (totalCost == null && quantityLiters != null && pricePerLiter != null) {
            totalCost = quantityLiters.multiply(pricePerLiter);
        }
    }
}
