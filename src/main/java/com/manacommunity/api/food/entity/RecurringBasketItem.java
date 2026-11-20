package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "recurring_basket_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringBasketItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long basketId;
    private Long productId;
    private String productType;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private boolean acceptSubstitutes;
}
