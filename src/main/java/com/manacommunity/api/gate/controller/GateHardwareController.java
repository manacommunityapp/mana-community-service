package com.manacommunity.api.gate.controller;

import com.manacommunity.api.gate.adapter.BarrierCommand;
import com.manacommunity.api.gate.adapter.BarrierCommandResult;
import com.manacommunity.api.gate.adapter.HardwareType;
import com.manacommunity.api.gate.adapter.RawHardwareEvent;
import com.manacommunity.api.gate.model.GateEvent;
import com.manacommunity.api.gate.model.RfidTagMapping;
import com.manacommunity.api.gate.repository.GateEventRepository;
import com.manacommunity.api.gate.repository.RfidTagMappingRepository;
import com.manacommunity.api.gate.service.GateHardwareDispatcher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gate")
@RequiredArgsConstructor
public class GateHardwareController {

    private final GateHardwareDispatcher dispatcher;
    private final GateEventRepository gateEventRepository;
    private final RfidTagMappingRepository rfidTagMappingRepository;

    // ─── Inbound hardware event ──────────────────────────────────────────────

    @PostMapping("/hardware/event")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<GateEvent> receiveHardwareEvent(@RequestBody @Valid RawHardwareEvent event) {
        GateEvent saved = dispatcher.dispatch(event);
        return ResponseEntity.ok(saved);
    }

    // ─── Outbound barrier command ────────────────────────────────────────────

    @PostMapping("/hardware/command")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<BarrierCommandResult> sendBarrierCommand(
            @RequestBody @Valid BarrierCommandRequest req) {
        BarrierCommand command = BarrierCommand.builder()
                .communityId(req.getCommunityId())
                .gateId(req.getGateId())
                .action(req.getAction())
                .reason(req.getReason())
                .triggeredBy(req.getTriggeredBy())
                .build();

        HardwareType target = req.getHardwareType() != null
                ? HardwareType.valueOf(req.getHardwareType())
                : HardwareType.BOOM_BARRIER;

        return ResponseEntity.ok(dispatcher.issueCommand(target, command));
    }

    // ─── Gate event log ──────────────────────────────────────────────────────

    @GetMapping("/events/{communityId}")
    @PreAuthorize("hasAnyAuthority('Manage Gate', 'View Gate')")
    public ResponseEntity<Page<GateEvent>> listGateEvents(
            @PathVariable Long communityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<GateEvent> events = gateEventRepository.findByCommunityIdOrderByCreatedAtDesc(
                communityId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(events);
    }

    // ─── RFID tag management ─────────────────────────────────────────────────

    @GetMapping("/rfid-tags/{communityId}")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<List<RfidTagMapping>> listRfidTags(@PathVariable Long communityId) {
        return ResponseEntity.ok(
                rfidTagMappingRepository.findByCommunityIdAndActiveTrueOrderByCreatedAtDesc(communityId));
    }

    @PostMapping("/rfid-tags/{communityId}")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<RfidTagMapping> registerRfidTag(
            @PathVariable Long communityId,
            @RequestBody @Valid RfidTagRequest req) {
        RfidTagMapping mapping = RfidTagMapping.builder()
                .communityId(communityId)
                .rfidTagId(req.getRfidTagId())
                .flatNumber(req.getFlatNumber())
                .residentId(req.getResidentId())
                .residentName(req.getResidentName())
                .vehicleType(req.getVehicleType())
                .active(true)
                .build();
        return ResponseEntity.ok(rfidTagMappingRepository.save(mapping));
    }

    @DeleteMapping("/rfid-tags/{communityId}/{id}")
    @PreAuthorize("hasAuthority('Manage Gate')")
    public ResponseEntity<Void> deactivateRfidTag(
            @PathVariable Long communityId,
            @PathVariable Long id) {
        rfidTagMappingRepository.findById(id).ifPresent(tag -> {
            tag.setActive(false);
            rfidTagMappingRepository.save(tag);
        });
        return ResponseEntity.noContent().build();
    }

    // ─── Request DTOs ────────────────────────────────────────────────────────

    @Data
    public static class BarrierCommandRequest {
        @NotBlank private String gateId;
        private Long communityId;
        @NotBlank private String action;
        private String reason;
        private String triggeredBy;
        private String hardwareType;
        private Long triggeredByPassId;
    }

    @Data
    public static class RfidTagRequest {
        @NotBlank private String rfidTagId;
        private String flatNumber;
        private Long residentId;
        private String residentName;
        private String vehicleType;
    }
}
