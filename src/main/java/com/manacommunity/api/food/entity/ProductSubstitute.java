package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "product_substitute")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSubstitute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long communityId;
    private Long vendorId;
    private Long originalProductId;
    private String originalProductType;
    private Long substituteProductId;
    private String substituteProductType;
    private int priority;
    private boolean isActive;
    private Instant createdAt;
}
