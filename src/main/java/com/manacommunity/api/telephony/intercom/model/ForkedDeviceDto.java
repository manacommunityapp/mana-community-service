package com.manacommunity.api.telephony.intercom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForkedDeviceDto {

    public enum Platform {
        IOS,
        ANDROID
    }

    public enum RingStatus {
        RINGING,
        ANSWERED,
        CANCELLED_ANSWERED_ELSEWHERE,
        FAILED
    }

    private String deviceId;
    private Long residentId;
    private String residentName;
    private Platform platform;
    private String pushToken;
    private RingStatus ringStatus;
    private String cancellationReason;
    private boolean missedCallBadged;
}
