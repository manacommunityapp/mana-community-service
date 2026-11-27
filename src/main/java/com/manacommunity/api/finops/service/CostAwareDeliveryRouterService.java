package com.manacommunity.api.finops.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.finops.dto.FinOpsDtos.*;
import com.manacommunity.api.finops.entity.DeliveryRoutingEventEntity;
import com.manacommunity.api.finops.entity.ResidentChannelProfileEntity;
import com.manacommunity.api.finops.repository.DeliveryRoutingEventRepository;
import com.manacommunity.api.finops.repository.ResidentChannelProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Intelligent Cost-Aware Telecom & Messaging Delivery Router.
 * Balances channel unit costs (FCM ₹0.00, WhatsApp ₹0.15, DLT SMS ₹0.20, PSTN IVR ₹0.40)
 * against a strict 15-second approval SLA using resident response telemetry and Redis CAS cancellation.
 */
@Slf4j
@Service
public class CostAwareDeliveryRouterService {

    public static final String CH_PUSH = "FCM_PUSH";
    public static final String CH_WA = "WHATSAPP_UTILITY";
    public static final String CH_SMS = "DLT_SMS";
    public static final String CH_IVR = "PSTN_IVR";

    public static final int COST_PUSH_PAISE = 0;
    public static final int COST_WA_PAISE = 15;
    public static final int COST_SMS_PAISE = 20;
    public static final int COST_IVR_PAISE = 40;
    public static final int DEFAULT_SLA_MS = 15_000;

    private static final DefaultRedisScript<Long> CAS_TRANSITION_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if current ~= ARGV[1] then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[2], 'EX', tonumber(ARGV[3]))
            return 1
            """,
            Long.class
    );

    private static final DefaultRedisScript<Long> RESOLVE_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if not current or current == 'RESOLVED' then
                return 0
            end
            redis.call('SET', KEYS[1], 'RESOLVED', 'EX', 300)
            return 1
            """,
            Long.class
    );

    private final ResidentChannelProfileRepository profileRepository;
    private final DeliveryRoutingEventRepository eventRepository;
    private final StringRedisTemplate redis;
    private final Map<String, String> localAlertStates = new ConcurrentHashMap<>();

    public CostAwareDeliveryRouterService(
            ResidentChannelProfileRepository profileRepository,
            DeliveryRoutingEventRepository eventRepository,
            ObjectProvider<StringRedisTemplate> redisProvider) {
        this.profileRepository = profileRepository;
        this.eventRepository = eventRepository;
        this.redis = redisProvider.getIfAvailable();
    }

    @Transactional
    public DeliveryRoutePlanDto dispatchAlert(DispatchAlertRequest req) {
        int slaMs = req.slaMs() > 0 ? req.slaMs() : DEFAULT_SLA_MS;
        ResidentChannelProfileEntity profile = profileRepository.findById(req.residentId())
                .orElseGet(() -> {
                    ResidentChannelProfileEntity p = new ResidentChannelProfileEntity();
                    p.setResidentId(req.residentId());
                    p.setSocietyId(req.societyId());
                    return profileRepository.save(p);
                });

        boolean wsRecent = req.wsOnlineOverride()
                || (profile.getWsLastSeenAt() != null
                && profile.getWsLastSeenAt().isAfter(Instant.now().minusSeconds(900)));
        boolean aggressiveDoze = req.aggressiveDozeOverride() || profile.isOemAggressiveDoze();
        boolean waOptIn = req.waOptInOverride() != null ? req.waOptInOverride() : profile.isWaOptIn();
        boolean explorationProbe = req.forceExplorationProbe() || ThreadLocalRandom.current().nextInt(100) < 5;

        List<EscalationStepDto> path = buildEscalationPlan(profile, slaMs, wsRecent, aggressiveDoze, waOptIn, explorationProbe);
        EscalationStepDto first = path.get(0);
        String reason = explainPlan(profile, wsRecent, aggressiveDoze, waOptIn, explorationProbe, first.channel());

        UUID alertId = UUID.randomUUID();
        DeliveryRoutingEventEntity event = new DeliveryRoutingEventEntity();
        event.setAlertId(alertId);
        event.setSocietyId(req.societyId());
        event.setResidentId(req.residentId());
        event.setVisitorType(req.visitorType() != null ? req.visitorType() : "DELIVERY");
        event.setSlaMs(slaMs);
        event.setSelectedInitialChannel(first.channel());
        event.setCurrentChannel(first.channel());
        event.setEscalationPathJson(path);
        event.setTotalCostPaise(first.costPaise());
        event.setBaselineIvrCostPaise(COST_IVR_PAISE);
        event.setSavingsPaise(Math.max(0, COST_IVR_PAISE - first.costPaise()));
        event.setStatus("ACTIVE_" + first.channel());
        event.setRoutingReason(reason);
        eventRepository.save(event);

        setAlertState(alertId.toString(), event.getStatus());

        return new DeliveryRoutePlanDto(
                alertId.toString(),
                req.societyId(),
                req.residentId(),
                slaMs,
                first.channel(),
                first.channel(),
                first.costPaise(),
                COST_IVR_PAISE,
                Math.max(0, COST_IVR_PAISE - first.costPaise()),
                event.getStatus(),
                reason,
                explorationProbe,
                path
        );
    }

    @Transactional
    public DeliveryEventDto escalateNext(String alertIdStr) {
        UUID alertId = UUID.fromString(alertIdStr);
        DeliveryRoutingEventEntity event = eventRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryRoutingEvent", "alertId", alertIdStr));

        if ("RESOLVED".equals(event.getStatus())) {
            return toEventDto(event);
        }

        List<EscalationStepDto> path = event.getEscalationPathJson();
        int currentIdx = -1;
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i).channel().equals(event.getCurrentChannel())) {
                currentIdx = i;
                break;
            }
        }
        if (currentIdx < 0 || currentIdx + 1 >= path.size()) {
            return toEventDto(event);
        }

        EscalationStepDto nextStep = path.get(currentIdx + 1);
        String expectedState = "ACTIVE_" + event.getCurrentChannel();
        String targetState = "ACTIVE_" + nextStep.channel();

        if (!casTransitionState(alertIdStr, expectedState, targetState)) {
            return toEventDto(eventRepository.findById(alertId).orElse(event));
        }

        event.setCurrentChannel(nextStep.channel());
        event.setTotalCostPaise(event.getTotalCostPaise() + nextStep.costPaise());
        event.setSavingsPaise(Math.max(0, event.getBaselineIvrCostPaise() - event.getTotalCostPaise()));
        event.setStatus(targetState);
        eventRepository.save(event);

        return toEventDto(event);
    }

    @Transactional
    public DeliveryEventDto ackAlert(String alertIdStr, AckAlertRequest req) {
        UUID alertId = UUID.fromString(alertIdStr);
        DeliveryRoutingEventEntity event = eventRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryRoutingEvent", "alertId", alertIdStr));

        if (!resolveAlertState(alertIdStr) && "RESOLVED".equals(event.getStatus())) {
            return toEventDto(event);
        }

        String channel = req.channel() != null ? req.channel() : event.getCurrentChannel();
        int latency = Math.max(100, req.latencyMs());
        boolean slaMet = latency <= event.getSlaMs();

        event.setWinningChannel(channel);
        event.setResolvedLatencyMs(latency);
        event.setSlaMet(slaMet);
        event.setStatus("RESOLVED");
        event.setResolvedAt(Instant.now());
        event.setSavingsPaise(Math.max(0, event.getBaselineIvrCostPaise() - event.getTotalCostPaise()));
        eventRepository.save(event);

        updateResidentProfileEma(event.getResidentId(), channel, latency, slaMet);
        return toEventDto(event);
    }

    @Transactional(readOnly = true)
    public TelecomFinOpsSummaryDto getSummary(long societyId) {
        List<DeliveryRoutingEventEntity> all = eventRepository.findBySocietyId(societyId);
        long resolved = all.stream().filter(e -> "RESOLVED".equals(e.getStatus())).count();
        long slaMet = all.stream().filter(e -> Boolean.TRUE.equals(e.getSlaMet())).count();
        double slaPct = resolved > 0 ? (slaMet * 100.0) / resolved : 100.0;
        double avgLatency = all.stream()
                .map(DeliveryRoutingEventEntity::getResolvedLatencyMs)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        long actualSpend = all.stream().mapToLong(DeliveryRoutingEventEntity::getTotalCostPaise).sum();
        long baselineSpend = all.stream().mapToLong(DeliveryRoutingEventEntity::getBaselineIvrCostPaise).sum();

        return new TelecomFinOpsSummaryDto(
                societyId,
                all.size(),
                resolved,
                slaPct,
                avgLatency,
                actualSpend,
                baselineSpend,
                Math.max(0L, baselineSpend - actualSpend)
        );
    }

    @Transactional(readOnly = true)
    public List<DeliveryEventDto> listEvents(long societyId) {
        return eventRepository.findTop50BySocietyIdOrderByCreatedAtDesc(societyId).stream()
                .map(this::toEventDto)
                .toList();
    }

    public List<EscalationStepDto> buildEscalationPlan(
            ResidentChannelProfileEntity p,
            int slaMs,
            boolean wsRecent,
            boolean aggressiveDoze,
            boolean waOptIn,
            boolean explorationProbe) {
        List<EscalationStepDto> steps = new ArrayList<>();
        boolean pushHealthy = wsRecent
                || (!aggressiveDoze && p.getFcmAckRate7d() >= 0.55 && p.getFcmP90Ms() <= 3800);

        if (pushHealthy || explorationProbe) {
            int pushWaitMs = Math.max(2200, Math.min(3800, p.getFcmP90Ms()));
            steps.add(new EscalationStepDto(CH_PUSH, COST_PUSH_PAISE, 0, pushWaitMs));
            if (waOptIn && p.getWaAckRate7d() >= 0.45) {
                int waWindow = Math.max(3500, (slaMs / 2) - pushWaitMs);
                steps.add(new EscalationStepDto(CH_WA, COST_WA_PAISE, pushWaitMs, waWindow));
                steps.add(new EscalationStepDto(CH_IVR, COST_IVR_PAISE, pushWaitMs + waWindow, slaMs - pushWaitMs - waWindow));
            } else {
                steps.add(new EscalationStepDto(CH_IVR, COST_IVR_PAISE, pushWaitMs, slaMs - pushWaitMs));
            }
            return steps;
        }

        // Predictive early-skip: Push is unreliable/killed; skip wasted Push wait!
        if (waOptIn && p.getWaAckRate7d() >= 0.60) {
            int waWindow = slaMs / 2;
            steps.add(new EscalationStepDto(CH_WA, COST_WA_PAISE, 0, waWindow));
            steps.add(new EscalationStepDto(CH_IVR, COST_IVR_PAISE, waWindow, slaMs - waWindow));
            return steps;
        }

        // Direct-to-IVR for offline/elderly/non-data resident -> saves ₹0.15 wasted WhatsApp attempt
        steps.add(new EscalationStepDto(CH_IVR, COST_IVR_PAISE, 0, slaMs));
        return steps;
    }

    private String explainPlan(
            ResidentChannelProfileEntity p,
            boolean wsRecent,
            boolean aggressiveDoze,
            boolean waOptIn,
            boolean explorationProbe,
            String initialChannel) {
        if (explorationProbe && !wsRecent && aggressiveDoze) {
            return "5% Thompson exploration probe: testing free FCM Push before IVR escalation";
        }
        return switch (initialChannel) {
            case CH_PUSH -> wsRecent
                    ? "Active WebSocket/heartbeat -> Free FCM Push (₹0.00) first"
                    : "Healthy FCM p90 (" + p.getFcmP90Ms() + "ms) -> Free FCM Push (₹0.00) first";
            case CH_WA -> "Predictive early-skip (OEM Doze/low FCM ack) -> Direct WhatsApp Utility (₹0.15)";
            default -> "Data/WhatsApp unreachable (" + (waOptIn ? "low WA ack" : "no WA opt-in") + ") -> Direct PSTN IVR (₹0.40)";
        };
    }

    private void updateResidentProfileEma(String residentId, String winningChannel, int latencyMs, boolean slaMet) {
        profileRepository.findById(residentId).ifPresent(profile -> {
            if (CH_PUSH.equals(winningChannel)) {
                profile.setFcmP50Ms((int) Math.round(profile.getFcmP50Ms() * 0.7 + latencyMs * 0.3));
                profile.setFcmP90Ms((int) Math.round(profile.getFcmP90Ms() * 0.7 + Math.max(latencyMs, 2200) * 0.3));
                profile.setFcmAckRate7d(Math.min(1.0, profile.getFcmAckRate7d() * 0.8 + (slaMet ? 0.2 : 0.0)));
                profile.setOemAggressiveDoze(false);
                profile.setPreferredChannel(CH_PUSH);
            } else if (CH_WA.equals(winningChannel)) {
                profile.setWaP50Ms((int) Math.round(profile.getWaP50Ms() * 0.7 + latencyMs * 0.3));
                profile.setWaAckRate7d(Math.min(1.0, profile.getWaAckRate7d() * 0.8 + (slaMet ? 0.2 : 0.0)));
                profile.setFcmAckRate7d(Math.max(0.0, profile.getFcmAckRate7d() * 0.85));
                profile.setPreferredChannel(CH_WA);
            } else if (CH_IVR.equals(winningChannel)) {
                profile.setFcmAckRate7d(Math.max(0.0, profile.getFcmAckRate7d() * 0.85));
                profile.setWaAckRate7d(Math.max(0.0, profile.getWaAckRate7d() * 0.85));
                profile.setIvrAckRate7d(Math.min(1.0, profile.getIvrAckRate7d() * 0.8 + (slaMet ? 0.2 : 0.0)));
                profile.setPreferredChannel(CH_IVR);
            }
            profile.setUpdatedAt(Instant.now());
            profileRepository.save(profile);
        });
    }

    private void setAlertState(String alertId, String state) {
        localAlertStates.put(alertId, state);
        if (redis != null) {
            try {
                redis.opsForValue().set("alert:route:" + alertId, state, Duration.ofMinutes(5));
            } catch (Exception ex) {
                log.debug("Redis alert state fallback: {}", ex.getMessage());
            }
        }
    }

    private boolean casTransitionState(String alertId, String expected, String next) {
        if (redis != null) {
            try {
                Long res = redis.execute(CAS_TRANSITION_SCRIPT, List.of("alert:route:" + alertId), expected, next, "300");
                if (res != null) {
                    if (res == 1L) localAlertStates.put(alertId, next);
                    return res == 1L;
                }
            } catch (Exception ex) {
                log.debug("Redis CAS fallback: {}", ex.getMessage());
            }
        }
        return localAlertStates.replace(alertId, expected, next);
    }

    private boolean resolveAlertState(String alertId) {
        if (redis != null) {
            try {
                Long res = redis.execute(RESOLVE_SCRIPT, List.of("alert:route:" + alertId));
                if (res != null) {
                    localAlertStates.put(alertId, "RESOLVED");
                    return res == 1L;
                }
            } catch (Exception ex) {
                log.debug("Redis resolve fallback: {}", ex.getMessage());
            }
        }
        String prev = localAlertStates.put(alertId, "RESOLVED");
        return prev != null && !"RESOLVED".equals(prev);
    }

    private DeliveryEventDto toEventDto(DeliveryRoutingEventEntity e) {
        return new DeliveryEventDto(
                e.getAlertId().toString(),
                e.getSocietyId(),
                e.getResidentId(),
                e.getVisitorType(),
                e.getSelectedInitialChannel(),
                e.getCurrentChannel(),
                e.getWinningChannel(),
                e.getResolvedLatencyMs(),
                e.getSlaMet(),
                e.getTotalCostPaise(),
                e.getSavingsPaise(),
                e.getStatus(),
                e.getRoutingReason(),
                e.getCreatedAt().toString()
        );
    }
}
