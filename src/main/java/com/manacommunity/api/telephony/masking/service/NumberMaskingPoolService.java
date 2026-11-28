package com.manacommunity.api.telephony.masking.service;

import com.manacommunity.api.telephony.masking.model.MaskedCallSessionDto;
import com.manacommunity.api.telephony.masking.model.RouteInboundCallResultDto;

import java.util.List;

public interface NumberMaskingPoolService {

    void initializePool(List<String> virtualDids);

    MaskedCallSessionDto allocateMaskedSession(Long communityId, String partyAPhone, String partyBPhone, int quarantineMinutes);

    MaskedCallSessionDto recordCallEnd(String sessionId);

    RouteInboundCallResultDto routeInboundCall(String callingPhone, String calledDid);

    int reclaimExpiredSessions();

    int getAvailableDidCount();

    MaskedCallSessionDto getSession(String sessionId);
}
