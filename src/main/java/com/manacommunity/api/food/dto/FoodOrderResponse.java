package com.manacommunity.api.food.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FoodOrderResponse {
    private Long id;
    private String orderNumber;
    private String providerType;
    private Long providerId;
    private String providerName;
    private Long userId;
    private String orderType;
    private String status;
    private String paymentStatus;
    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String deliveryInstructions;
    private LocalDateTime scheduledFor;
    private Boolean isGift;
    private String giftMessage;
    private String paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private String couponCode;
    private List<OrderItemResponse> items;
    private List<TrackingResponse> tracking;
    private RatingResponse rating;
    private Long communityId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String variantName;
        private String specialInstructions;
    }

    @Data
    @Builder
    public static class TrackingResponse {
        private Long id;
        private String status;
        private String description;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    public static class RatingResponse {
        private Long id;
        private Integer foodRating;
        private Integer deliveryRating;
        private String review;
    }
}
