package com.manacommunity.api.telephony.cascade.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CascadeExecutionResultDto {

    public enum Channel {
        WEBRTC_VOIP,
        GSM_PSTN_IVR,
        FLASH_SMS_WHATSAPP
    }

    public enum Outcome {
        APPROVED,
        DENIED,
        PENDING_RESPONSE,
        EXPIRED_NO_ANSWER
    }

    private String intercomId;
    private Long communityId;
    private Channel finalChannel;
    private Outcome approvalOutcome;
    private CallHealthMetricsDto webrtcHealth;
    private String pstnDtmfReceived;
    private String failoverReason;
    @Builder.Default
    private List<String> cascadeAuditLog = new ArrayList<>();
}
