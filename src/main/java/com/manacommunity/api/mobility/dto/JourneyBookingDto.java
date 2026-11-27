package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder
public class JourneyBookingDto {
    private Long id;
    private Long offerId;
    private Long userId;
    private String userName;
    private short seats;
    private String status;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String paymentOrderId;
    private String guardianConsentStatus;
    private String guardianName;
    private String guardianPhone;
    private Instant bookedAt;
    private MobilityCarpoolOfferDto offer;
    private boolean offerOwner;
}
