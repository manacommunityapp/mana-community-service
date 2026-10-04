package com.manacommunity.api.service;

import com.manacommunity.api.dto.AccessVerifyRequest;
import com.manacommunity.api.dto.TurnstileRequest;
import com.manacommunity.api.model.AccessLog;
import com.manacommunity.api.model.Turnstile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface BiometricAccessService {

    // Turnstiles
    List<Turnstile> getTurnstiles(Long communityId);
    Turnstile getTurnstile(Long communityId, Long id);
    Turnstile createTurnstile(Long communityId, TurnstileRequest request);
    Turnstile updateTurnstileStatus(Long communityId, Long id, String status);

    // Access logs
    List<AccessLog> getAccessLogs(Long communityId, Long userId, Long turnstileId,
                                   LocalDateTime from, LocalDateTime to);
    AccessLog verifyAccess(Long communityId, AccessVerifyRequest request);
    Map<String, Object> getAccessSummary(Long communityId);
}
