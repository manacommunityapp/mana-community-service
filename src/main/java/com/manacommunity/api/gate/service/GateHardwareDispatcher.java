package com.manacommunity.api.gate.service;

import com.manacommunity.api.gate.adapter.*;
import com.manacommunity.api.gate.model.GateEvent;
import com.manacommunity.api.gate.repository.GateEventRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Routes inbound hardware events to the matching adapter, persists a GateEvent, and
 * broadcasts the result over STOMP so guard tablets see real-time updates.
 *
 * Also handles outbound barrier commands (open/close a boom barrier or smart lock).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GateHardwareDispatcher {

    private final List<GateHardwareAdapter> adapters;
    private final GateEventRepository gateEventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<HardwareType, GateHardwareAdapter> adapterMap = new EnumMap<>(HardwareType.class);

    @PostConstruct
    void init() {
        adapters.forEach(a -> adapterMap.put(a.hardwareType(), a));
        log.info("GateHardwareDispatcher registered adapters: {}", adapterMap.keySet());
    }

    @Transactional
    public GateEvent dispatch(RawHardwareEvent event) {
        GateHardwareAdapter adapter = adapterMap.get(event.hardwareType());
        if (adapter == null) {
            log.error("No adapter registered for hardware type {}", event.hardwareType());
            throw new IllegalArgumentException("Unsupported hardware type: " + event.hardwareType());
        }

        GateEventResult result = adapter.process(event);

        GateEvent entity = GateEvent.builder()
                .communityId(event.communityId())
                .gateId(result.gateId())
                .hardwareType(result.hardwareType())
                .direction(result.direction())
                .identifierType(result.identifierType())
                .identifierValue(result.identifierValue())
                .matchedEntityType(result.matchedEntityType())
                .matchedEntityId(result.matchedEntityId())
                .matchedName(result.matchedName())
                .barrierAction(result.barrierAction())
                .notes(result.notes())
                .build();

        entity = gateEventRepository.save(entity);

        broadcastEvent(event.communityId(), entity);

        return entity;
    }

    public BarrierCommandResult issueCommand(BarrierCommand command) {
        // Commands go to boom barriers (via BoomBarrierAdapter) or smart locks (SmartLockAdapter).
        // Caller should pick the right HardwareType; fall back to BOOM_BARRIER if none specified.
        GateHardwareAdapter adapter = adapterMap.get(HardwareType.BOOM_BARRIER);
        if (adapter == null) {
            return BarrierCommandResult.builder()
                    .success(false)
                    .gateId(command.gateId())
                    .action(command.action())
                    .message("BoomBarrierAdapter not registered")
                    .build();
        }
        return adapter.sendCommand(command);
    }

    public BarrierCommandResult issueCommand(HardwareType targetType, BarrierCommand command) {
        GateHardwareAdapter adapter = adapterMap.get(targetType);
        if (adapter == null) {
            return BarrierCommandResult.builder()
                    .success(false)
                    .gateId(command.gateId())
                    .action(command.action())
                    .message("No adapter for type: " + targetType)
                    .build();
        }
        return adapter.sendCommand(command);
    }

    private void broadcastEvent(Long communityId, GateEvent event) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/community/" + communityId + "/gate/events",
                    event
            );
        } catch (Exception e) {
            log.warn("Failed to broadcast gate event {} via STOMP: {}", event.getId(), e.getMessage());
        }
    }
}
