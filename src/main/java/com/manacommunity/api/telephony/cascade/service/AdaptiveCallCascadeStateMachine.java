package com.manacommunity.api.telephony.cascade.service;

import com.manacommunity.api.telephony.cascade.model.CallHealthMetricsDto;
import com.manacommunity.api.telephony.cascade.model.CascadeExecutionResultDto;

public interface AdaptiveCallCascadeStateMachine {

    CascadeExecutionResultDto executeCascade(
            String intercomId,
            Long communityId,
            String flatId,
            String visitorName,
            String residentPhone,
            CallHealthMetricsDto webrtcHealth,
            String pstnResponseDtmf);
}
