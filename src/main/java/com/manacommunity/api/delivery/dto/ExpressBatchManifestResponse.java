package com.manacommunity.api.delivery.dto;

import com.manacommunity.api.delivery.model.DeliveryPartner;
import com.manacommunity.api.delivery.model.ExpressPassStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressBatchManifestResponse {
    private Boolean success;
    private Boolean allowed;
    private Long manifestId;
    private String manifestCode;
    private DeliveryPartner partner;
    private ExpressPassStatus status;
    private String riderName;
    private String riderPhone;
    private String vehicleNumber;
    private Integer totalPackages;
    private Integer distinctFlats;
    private Integer distinctFlatsNotified;
    private Integer deliveredPackages;
    private Integer pendingPackages;
    private String barrierAction;
    private String message;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
    private List<ExpressEntryResponse> packages;
}
