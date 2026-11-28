package com.manacommunity.api.telephony.intercom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSessionDto {

    public enum State {
        INITIATING,
        RINGING,
        ANSWERED,
        TERMINATED,
        EXPIRED
    }

    private String callId;
    private Long communityId;
    private String flatIdentifier;
    private String callerType; // GUARD_TABLET, DELIVERY_RIDER, VISITOR_KIOSK
    private String callerName;
    private String visitorName;
    private LocalDateTime initiatedAt;
    private State state;
    private String answeredByDeviceId;
    private LocalDateTime answeredAt;
    @Builder.Default
    private List<ForkedDeviceDto> forkedDevices = new ArrayList<>();
}
