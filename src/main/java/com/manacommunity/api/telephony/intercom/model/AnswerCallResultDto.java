package com.manacommunity.api.telephony.intercom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerCallResultDto {

    private boolean success;
    private String callId;
    private String answeringDeviceId;
    private List<String> cancelledDeviceIds;
    private long cancellationLatencyMs;
    private String message;
}
