package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketOrder;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOrderResponse {

    private Long id;
    private String orderNumber;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private Long communityId;
    private MarketOrder.OrderStatus status;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private String couponCode;
    private MarketOrder.PaymentMethod paymentMethod;
    private MarketOrder.PaymentStatus paymentStatus;
    private MarketOrder.DeliveryMode deliveryMode;
    private String deliveryAddress;
    private String notes;
    private List<OrderItemResponse> items;
    private MarketHandoverPassDto pass;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long listingId;
        private String listingTitle;
        private String listingCategory;
        private String imageUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
