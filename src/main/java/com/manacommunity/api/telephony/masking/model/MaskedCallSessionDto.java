package com.manacommunity.api.telephony.masking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaskedCallSessionDto {

    public enum State {
        ALLOCATED,
        IN_CALL,
        COOLING_DOWN,
        EXPIRED,
        RECLAIMED
    }

    private String sessionId;
    private Long communityId;
    private String partyAPhone; // Caller (e.g. Delivery Rider or Guard)
    private String partyBPhone; // Callee (e.g. Resident)
    private String virtualDid;  // Masked Number visible to both parties
    private State state;
    private LocalDateTime allocatedAt;
    private LocalDateTime callEndedAt;
    private LocalDateTime coolingExpiryTime;
    @Builder.Default
    private int lateCallbackCount = 0;
}
