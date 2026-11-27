package com.manacommunity.api.telephony.intercom.service;

import com.manacommunity.api.telephony.intercom.model.AnswerCallResultDto;
import com.manacommunity.api.telephony.intercom.model.CallSessionDto;
import com.manacommunity.api.telephony.intercom.model.ForkedDeviceDto;

import java.util.List;

public interface VoipSignalingOrchestrator {

    CallSessionDto initiateParallelRing(
            String flatIdentifier,
            Long communityId,
            String callerType,
            String callerName,
            String visitorName,
            List<ForkedDeviceDto> targetDevices);

    AnswerCallResultDto answerCall(String callId, String answeringDeviceId);

    CallSessionDto terminateCall(String callId, String reason);

    CallSessionDto getCallSession(String callId);
}
