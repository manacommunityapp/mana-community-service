package com.manacommunity.api.marketplace.dto;

import com.manacommunity.api.marketplace.entity.MarketOrder;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOrderRequest {

    @NotNull(message = "Seller ID is required")
    private Long sellerId;

    @NotEmpty(message = "Order items cannot be empty")
    private List<OrderItemRequest> items;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0")
    private BigDecimal totalAmount;

    private BigDecimal deliveryFee;
    private String couponCode;
    private MarketOrder.PaymentMethod paymentMethod;
    private MarketOrder.DeliveryMode deliveryMode;
    private String deliveryAddress;
    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "Listing ID is required")
        private Long listingId;
        @NotNull(message = "Quantity is required")
        private Integer quantity;
        @NotNull(message = "Unit price is required")
        private BigDecimal unitPrice;
    }
}
