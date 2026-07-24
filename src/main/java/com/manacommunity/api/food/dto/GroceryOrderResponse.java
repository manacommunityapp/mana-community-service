package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GroceryOrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private Long storeId;
    private String storeName;
    private String status;
    private String deliveryAddress;
    private String deliverySlot;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private List<GroceryOrderItemResponse> items;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class GroceryOrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
