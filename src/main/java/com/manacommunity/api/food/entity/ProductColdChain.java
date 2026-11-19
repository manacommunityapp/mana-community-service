package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_cold_chain")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductColdChain {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long vendorId;
    private Long productId;
    private String productType;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "cold_chain_type")
    private DeliveryBatchOrder.ColdChainType coldChainType;
}
