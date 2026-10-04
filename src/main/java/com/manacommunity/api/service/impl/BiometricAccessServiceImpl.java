package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.AccessVerifyRequest;
import com.manacommunity.api.dto.TurnstileRequest;
import com.manacommunity.api.model.AccessLog;
import com.manacommunity.api.model.Turnstile;
import com.manacommunity.api.repository.AccessLogRepository;
import com.manacommunity.api.repository.TurnstileRepository;
import com.manacommunity.api.service.BiometricAccessService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BiometricAccessServiceImpl implements BiometricAccessService {

    private final TurnstileRepository turnstileRepository;
    private final AccessLogRepository accessLogRepository;
    private final AppUserRepository appUserRepository;

    // ── Turnstiles ───────────────────────────────────────────────────────

    @Override
    public List<Turnstile> getTurnstiles(Long communityId) {
        return turnstileRepository.findByCommunityIdOrderByName(communityId);
    }

    @Override
    public Turnstile getTurnstile(Long communityId, Long id) {
        return turnstileRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Turnstile not found: " + id));
    }

    @Override
    @Transactional
    public Turnstile createTurnstile(Long communityId, TurnstileRequest request) {
        Turnstile turnstile = Turnstile.builder()
                .communityId(communityId)
                .name(request.name())
                .location(request.location())
                .type(Turnstile.TurnstileType.valueOf(request.type().toUpperCase()))
                .build();
        return turnstileRepository.save(turnstile);
    }

    @Override
    @Transactional
    public Turnstile updateTurnstileStatus(Long communityId, Long id, String status) {
        Turnstile turnstile = turnstileRepository.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Turnstile not found: " + id));
        turnstile.setStatus(Turnstile.TurnstileStatus.valueOf(status.toUpperCase()));
        turnstile.setLastHeartbeat(LocalDateTime.now());
        return turnstileRepository.save(turnstile);
    }

    // ── Access Logs ──────────────────────────────────────────────────────

    @Override
    public List<AccessLog> getAccessLogs(Long communityId, Long userId, Long turnstileId,
                                          LocalDateTime from, LocalDateTime to) {
        if (userId != null) {
            return accessLogRepository.findByTurnstileCommunityIdAndUserIdOrderByTimestampDesc(communityId, userId);
        }
        if (turnstileId != null) {
            return accessLogRepository.findByTurnstileIdOrderByTimestampDesc(turnstileId);
        }
        if (from != null && to != null) {
            return accessLogRepository.findByTurnstileCommunityIdAndTimestampBetweenOrderByTimestampDesc(
                    communityId, from, to);
        }
        return accessLogRepository.findByTurnstileCommunityIdOrderByTimestampDesc(communityId);
    }

    @Override
    @Transactional
    public AccessLog verifyAccess(Long communityId, AccessVerifyRequest request) {
        Turnstile turnstile = turnstileRepository.findByIdAndCommunityId(request.turnstileId(), communityId)
                .orElseThrow(() -> new IllegalArgumentException("Turnstile not found: " + request.turnstileId()));

        AppUser user = null;
        if (request.userId() != null) {
            user = appUserRepository.findById(request.userId()).orElse(null);
        }

        AccessLog log = AccessLog.builder()
                .turnstile(turnstile)
                .user(user)
                .direction(AccessLog.AccessDirection.valueOf(request.direction().toUpperCase()))
                .method(AccessLog.AccessMethod.valueOf(request.method().toUpperCase()))
                .timestamp(LocalDateTime.now())
                .granted(request.granted() != null ? request.granted() : true)
                .denialReason(request.denialReason())
                .build();

        return accessLogRepository.save(log);
    }

    @Override
    public Map<String, Object> getAccessSummary(Long communityId) {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGranted", accessLogRepository.countByTurnstileCommunityIdAndGrantedTrue(communityId));
        summary.put("totalDenied", accessLogRepository.countByTurnstileCommunityIdAndGrantedFalse(communityId));
        summary.put("entriesToday", accessLogRepository.countByTurnstileCommunityIdAndDirectionAndTimestampAfter(
                communityId, AccessLog.AccessDirection.ENTRY, today));
        summary.put("exitsToday", accessLogRepository.countByTurnstileCommunityIdAndDirectionAndTimestampAfter(
                communityId, AccessLog.AccessDirection.EXIT, today));
        return summary;
    }
}
