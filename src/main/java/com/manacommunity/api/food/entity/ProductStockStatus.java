package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "product_stock_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockStatus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long communityId;
    private Long vendorId;
    private Long productId;
    private String productType;
    private boolean isInStock;
    private LocalDate restockDate;
    private Long updatedBy;
    private Instant updatedAt;
}
