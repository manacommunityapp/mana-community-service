package com.manacommunity.api.groupbuying.dto;

import com.manacommunity.api.groupbuying.model.OrderStatus;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupOrderResponse {
    private String id;
    private String dealId;
    private String title;
    private String category;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal total;
    private BigDecimal savings;
    private OrderStatus status;
    private String qrCode;
    private String pickupPoint;
    private String pickupDate;
    private String pickupSlot;
    private String createdAt;
}
