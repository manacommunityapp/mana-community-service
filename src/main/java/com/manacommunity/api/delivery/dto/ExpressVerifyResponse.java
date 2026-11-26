package com.manacommunity.api.delivery.dto;

import com.manacommunity.api.delivery.model.DeliveryPartner;
import com.manacommunity.api.delivery.model.ExpressPassStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressVerifyResponse {
    private Boolean allowed;
    private String message;
    private Long passId;
    private String orderId;
    private DeliveryPartner partner;
    private String riderName;
    private String riderPhone;
    private String vehicleNumber;
    private String flatNumber;
    private String residentName;
    private Boolean leaveAtGate;
    private String gateInstructions;
    private ExpressPassStatus status;
    private String barrierAction; // e.g. "OPEN_BOOM_BARRIER" or "HOLD"
    private LocalDateTime timestamp;
}
