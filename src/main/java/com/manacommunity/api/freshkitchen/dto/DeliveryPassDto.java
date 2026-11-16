package com.manacommunity.api.freshkitchen.dto;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPassDto {
    private Long orderId;
    private String orderNumber;
    private Long buyerId;
    private String buyerName;
    private String deliveryAddress;
    private String deliverySlot;
    private LocalDate estimatedDeliveryDate;
    private String status;
    private String handoverOtp;
    private String qrToken;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String paymentMethod;
    private Boolean isEscrowLocked;
    private LocalDateTime actualDeliveryAt;
    private List<PassItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassItemDto {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    public static DeliveryPassDto fromEntity(CommerceOrder order) {
        if (order == null) return null;

        List<PassItemDto> itemDtos = order.getItems() != null
                ? order.getItems().stream().map(i -> PassItemDto.builder()
                        .productId(i.getProductId())
                        .productName(i.getTitle())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .totalPrice(i.getTotalPrice())
                        .build()).toList()
                : Collections.emptyList();

        return DeliveryPassDto.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .buyerId(order.getBuyer() != null ? order.getBuyer().getId() : null)
                .buyerName(order.getBuyer() != null ? order.getBuyer().getFullName() : null)
                .deliveryAddress(order.getDeliveryAddress())
                .deliverySlot(order.getPickupSlot())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .handoverOtp(order.getHandoverOtp())
                .qrToken(order.getQrToken())
                .totalAmount(order.getTotalAmount())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .isEscrowLocked(order.getIsEscrowLocked())
                .actualDeliveryAt(order.getActualDeliveryAt())
                .items(itemDtos)
                .build();
    }
}
