package com.manacommunity.api.delivery.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentDeliveryPreferenceDto {
    private Long id;
    private Long userId;
    private Long communityId;
    private String flatNumber;
    private Boolean autoApproveSwiggy;
    private Boolean autoApproveZomato;
    private Boolean autoApproveBlinkit;
    private Boolean autoApproveZepto;
    private Boolean autoApproveAmazon;
    private Boolean autoApproveOther;
    private Boolean defaultLeaveAtGate;
    private String deliveryInstructions;
}
