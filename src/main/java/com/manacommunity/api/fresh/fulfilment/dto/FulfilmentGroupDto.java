package com.manacommunity.api.fresh.fulfilment.dto;

import com.manacommunity.api.fresh.catalog.model.DeliveryMode;
import com.manacommunity.api.fresh.catalog.model.FulfilmentOwnerType;
import com.manacommunity.api.fresh.fulfilment.model.FulfilmentGroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfilmentGroupDto {
    private Long id;
    private String groupReference;
    private Long orderId;
    private String orderNumber;
    private Integer groupNumber;
    private Long vendorId;
    private String vendorBusinessName;
    private FulfilmentOwnerType fulfilmentOwnerType;
    private DeliveryMode deliveryMode;
    private FulfilmentGroupStatus status;
    private Long fulfilmentLocationId;
    private String fulfilmentLocationName;
    private Instant targetDeliveryTime;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    private String trackingReference;
    private String handoverOtp;
    private String qrCodeToken;
    private BigDecimal subtotalAmount;
    private BigDecimal totalWeightG;
    private Boolean requiresColdChain;
    private List<FulfilmentItemDto> items;
    private Instant createdAt;
    private Instant updatedAt;
}
