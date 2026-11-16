package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class JourneyBookingDto {
    private Long id;
    private Long offerId;
    private Long userId;
    private String userName;
    private short seats;
    private String status;
    private Instant bookedAt;
    private MobilityCarpoolOfferDto offer;
    private boolean offerOwner;
}
