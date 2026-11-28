package com.manacommunity.api.delivery.dto;

import com.manacommunity.api.delivery.model.DeliveryPartner;
import com.manacommunity.api.delivery.model.ExpressPassStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressEntryResponse {
    private Boolean success;
    private Long id;
    private String orderId;
    private DeliveryPartner partner;
    private String entryPassCode;
    private String barcodePayload;
    private ExpressPassStatus status;
    private String flatNumber;
    private String riderName;
    private String vehicleNumber;
    private Boolean leaveAtGate;
    private String gateInstructions;
    private String message;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
}
