package com.manacommunity.api.groupbuying.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReviewResponse {
    private String id;
    private String orderId;
    private String dealId;
    private String residentName;
    private Integer productRating;
    private Integer deliveryRating;
    private String comment;
    private String createdAt;
}
