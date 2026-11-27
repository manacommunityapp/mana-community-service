package com.manacommunity.api.delivery.dto;

import com.manacommunity.api.delivery.model.DeliveryPartner;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressEntryDispatchRequest {

    @NotNull(message = "partner is required (SWIGGY, ZOMATO, BLINKIT, ZEPTO, AMAZON, etc.)")
    private DeliveryPartner partner;

    @NotBlank(message = "orderId is required")
    private String orderId;

    @NotNull(message = "communityId is required")
    private Long communityId;

    @NotBlank(message = "flatNumber is required")
    private String flatNumber;

    private String tower;
    private String residentName;
    private String residentPhone;

    @NotBlank(message = "riderName is required")
    private String riderName;

    private String riderPhone;
    private String vehicleNumber;
    private String riderPhotoUrl;
    private Integer etaMinutes;
    private String itemsSummary;
}
