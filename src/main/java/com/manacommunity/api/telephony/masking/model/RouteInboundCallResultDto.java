package com.manacommunity.api.telephony.masking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteInboundCallResultDto {

    public enum RoutingStatus {
        ROUTED_ACTIVE_SESSION,
        ROUTED_QUARANTINE_CALLBACK,
        REJECTED_EXPIRED_IVR,
        REJECTED_UNKNOWN_CALLER
    }

    private boolean success;
    private String inboundCallingPhone;
    private String inboundCalledDid;
    private String routeDestinationPhone;
    private RoutingStatus status;
    private String ivrMessage;
}
