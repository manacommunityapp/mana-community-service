package com.manacommunity.api.marketplace.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private OrderBuyerRef buyer;
    private OrderSellerRef seller;
    private String status;
    private BigDecimal totalAmount;
    private String notes;
    private String deliveryAddress;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class OrderBuyerRef {
        private Long id;
        private String fullName;
    }

    @Data
    @Builder
    public static class OrderSellerRef {
        private Long id;
        private String fullName;
        private boolean verified;
    }

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private Long listingId;
        private String listingTitle;
        private int quantity;
        private BigDecimal unitPrice;
        private String imageUrl;
    }
}
