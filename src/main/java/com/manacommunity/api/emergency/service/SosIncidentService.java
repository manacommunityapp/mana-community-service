package com.manacommunity.api.emergency.service;

import com.manacommunity.api.emergency.dto.*;
import com.manacommunity.api.emergency.engine.GateLockdownEngine;
import com.manacommunity.api.emergency.engine.SosSlaEngine;
import com.manacommunity.api.emergency.entity.GateLockdownLog;
import com.manacommunity.api.emergency.entity.GateLockdownLog.LockdownDirective;
import com.manacommunity.api.emergency.entity.SosDispatch;
import com.manacommunity.api.emergency.entity.SosIncident;
import com.manacommunity.api.emergency.entity.SosIncident.EmergencyType;
import com.manacommunity.api.emergency.entity.SosIncident.IncidentStatus;
import com.manacommunity.api.emergency.entity.SosIncident.Severity;
import com.manacommunity.api.emergency.repository.GateLockdownLogRepository;
import com.manacommunity.api.emergency.repository.SosDispatchRepository;
import com.manacommunity.api.emergency.repository.SosIncidentRepository;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SosIncidentService {

    private final SosIncidentRepository incidentRepository;
    private final SosDispatchRepository dispatchRepository;
    private final GateLockdownLogRepository lockdownLogRepository;
    private final SosSlaEngine slaEngine;
    private final GateLockdownEngine lockdownEngine;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Panic Trigger & Response Lifecycle ───────────────────────────────────

    @Transactional
    public SosIncidentResponse triggerSos(AppUser resident, SosTriggerRequest request) {
        Community community = resident.getCommunity();
        EmergencyType type = request.emergencyType() != null ? request.emergencyType() : EmergencyType.GENERAL_PANIC;
        Severity severity = request.severity() != null ? request.severity() : Severity.CRITICAL;

        int targetSlaSeconds = slaEngine.getTargetResponseSeconds(severity);

        // Evaluate automated gate lockdown recommendation
        LockdownDirective autoDirective = lockdownEngine.evaluateDirective(type);
        boolean shouldLockdown = Boolean.TRUE.equals(request.triggerGateLockdown())
                || autoDirective == LockdownDirective.LOCKDOWN_CLOSE_ALL
                || autoDirective == LockdownDirective.EVACUATION_OPEN_ALL;

        SosIncident incident = SosIncident.builder()
                .community(community)
                .resident(resident)
                .flatNumber(request.flatNumber())
                .buildingBlock(request.buildingBlock())
                .emergencyType(type)
                .severity(severity)
                .status(IncidentStatus.TRIGGERED)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .locationDetails(request.locationDetails())
                .notes(request.notes())
                .lockdownInitiated(shouldLockdown)
                .slaTargetSeconds(targetSlaSeconds)
                .triggeredAt(LocalDateTime.now())
                .build();

        incident = incidentRepository.save(incident);

        if (shouldLockdown) {
            GateLockdownLog logEntry = GateLockdownLog.builder()
                    .community(community)
                    .incident(incident)
                    .directive(autoDirective != LockdownDirective.NORMAL_RESTORE ? autoDirective : LockdownDirective.LOCKDOWN_CLOSE_ALL)
                    .reason("Automated trigger from " + type + " emergency SOS #" + incident.getId())
                    .initiatedBy(resident)
                    .createdAt(LocalDateTime.now())
                    .build();
            lockdownLogRepository.save(logEntry);
        }

        broadcastEmergencyAlert(community.getId(), incident);

        log.warn("EMERGENCY SOS TRIGGERED: id={} resident={} type={} severity={} lockdown={}",
                incident.getId(), resident.getFullName(), type, severity, shouldLockdown);

        return SosIncidentResponse.from(incident);
    }

    @Transactional
    public SosIncidentResponse acknowledgeSos(AppUser guard, Long incidentId) {
        SosIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("SosIncident", incidentId));

        if (incident.getStatus() == IncidentStatus.TRIGGERED) {
            incident.setStatus(IncidentStatus.ACKNOWLEDGED);
            incident.setAcknowledgedAt(LocalDateTime.now());
            incident = incidentRepository.save(incident);
            broadcastEmergencyAlert(incident.getCommunity().getId(), incident);
        }

        return SosIncidentResponse.from(incident);
    }

    @Transactional
    public SosIncidentResponse dispatchGuard(Long incidentId, AppUser guard, String notes) {
        SosIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("SosIncident", incidentId));

        SosDispatch dispatch = SosDispatch.builder()
                .incident(incident)
                .guard(guard)
                .dispatchedAt(LocalDateTime.now())
                .guardNotes(notes)
                .build();

        dispatchRepository.save(dispatch);

        incident.setStatus(IncidentStatus.DISPATCHED);
        if (incident.getAcknowledgedAt() == null) {
            incident.setAcknowledgedAt(LocalDateTime.now());
        }
        incident = incidentRepository.save(incident);

        broadcastEmergencyAlert(incident.getCommunity().getId(), incident);

        log.info("Guard {} dispatched to Emergency Incident #{}", guard.getFullName(), incidentId);
        return SosIncidentResponse.from(incident);
    }

    @Transactional
    public SosIncidentResponse recordGuardArrival(AppUser guard, Long incidentId, String notes) {
        SosIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("SosIncident", incidentId));

        LocalDateTime arrivedAt = LocalDateTime.now();
        incident.setArrivedAt(arrivedAt);
        incident.setStatus(IncidentStatus.ON_SITE);

        var slaResult = slaEngine.evaluateResponseSla(incident.getTriggeredAt(), arrivedAt, incident.getSlaTargetSeconds());

        // Update matching guard dispatch record
        dispatchRepository.findByIncidentId(incidentId).stream()
                .filter(d -> d.getGuard().getId().equals(guard.getId()))
                .findFirst()
                .ifPresent(d -> {
                    d.setArrivedAt(arrivedAt);
                    d.setResponseDurationSeconds(slaResult.elapsedSeconds());
                    d.setSlaBreached(slaResult.isBreached());
                    if (notes != null) d.setGuardNotes(notes);
                    dispatchRepository.save(d);
                });

        incident = incidentRepository.save(incident);
        broadcastEmergencyAlert(incident.getCommunity().getId(), incident);

        log.info("Guard arrival logged for SOS #{}: elapsedSeconds={} breached={}",
                incidentId, slaResult.elapsedSeconds(), slaResult.isBreached());

        return SosIncidentResponse.from(incident);
    }

    @Transactional
    public SosIncidentResponse resolveSos(AppUser resolver, Long incidentId, SosResolveRequest request) {
        SosIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("SosIncident", incidentId));

        incident.setStatus(request.status() != null ? request.status() : IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolutionNotes(request.resolutionNotes());
        incident.setResolvedBy(resolver);

        if (Boolean.TRUE.equals(request.restoreGatesToNormal()) && incident.isLockdownInitiated()) {
            GateLockdownLog restoreLog = GateLockdownLog.builder()
                    .community(incident.getCommunity())
                    .incident(incident)
                    .directive(LockdownDirective.NORMAL_RESTORE)
                    .reason("Restoration following resolution of SOS #" + incidentId)
                    .initiatedBy(resolver)
                    .createdAt(LocalDateTime.now())
                    .build();
            lockdownLogRepository.save(restoreLog);
            incident.setLockdownInitiated(false);
        }

        incident = incidentRepository.save(incident);
        broadcastEmergencyAlert(incident.getCommunity().getId(), incident);

        log.info("Emergency Incident #{} marked as {}: resolvedBy={}",
                incidentId, incident.getStatus(), resolver.getFullName());

        return SosIncidentResponse.from(incident);
    }

    // ── Manual Gate Lockdown Control ───────────────────────────────────────────

    @Transactional
    public GateLockdownLog executeGateLockdown(AppUser user, GateLockdownRequest request) {
        SosIncident incident = null;
        if (request.incidentId() != null) {
            incident = incidentRepository.findById(request.incidentId()).orElse(null);
        }

        GateLockdownLog logEntry = GateLockdownLog.builder()
                .community(user.getCommunity())
                .incident(incident)
                .directive(request.directive())
                .affectedGates(request.affectedGates() != null ? request.affectedGates() : "ALL_GATES")
                .reason(request.reason())
                .initiatedBy(user)
                .createdAt(LocalDateTime.now())
                .build();

        return lockdownLogRepository.save(logEntry);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SosIncidentResponse> getActiveIncidents(Long communityId) {
        List<IncidentStatus> active = List.of(
                IncidentStatus.TRIGGERED, IncidentStatus.ACKNOWLEDGED,
                IncidentStatus.DISPATCHED, IncidentStatus.ON_SITE
        );
        return incidentRepository.findByCommunityIdAndStatusInOrderByTriggeredAtDesc(communityId, active)
                .stream()
                .map(SosIncidentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SosIncidentResponse> getIncidentHistory(Long communityId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("triggeredAt").descending());
        return incidentRepository.findByCommunityIdOrderByTriggeredAtDesc(communityId, pageable)
                .map(SosIncidentResponse::from);
    }

    @Transactional(readOnly = true)
    public List<SosIncidentResponse> getMyIncidents(Long residentId) {
        return incidentRepository.findByResidentIdOrderByTriggeredAtDesc(residentId)
                .stream()
                .map(SosIncidentResponse::from)
                .toList();
    }

    // ── WebSocket Broadcaster ──────────────────────────────────────────────────

    private void broadcastEmergencyAlert(Long communityId, SosIncident incident) {
        try {
            String location = (incident.getBuildingBlock() != null ? incident.getBuildingBlock() + " - " : "") +
                    (incident.getFlatNumber() != null ? "Flat " + incident.getFlatNumber() : (incident.getLocationDetails() != null ? incident.getLocationDetails() : "Campus"));

            SosBroadcastAlert alert = new SosBroadcastAlert(
                    incident.getId(),
                    incident.getEmergencyType().name(),
                    incident.getSeverity().name(),
                    incident.getResident().getFullName(),
                    incident.getResident().getPhone(),
                    location,
                    incident.getStatus().name(),
                    incident.isLockdownInitiated(),
                    incident.getTriggeredAt()
            );

            messagingTemplate.convertAndSend("/topic/community/" + communityId + "/emergency/sos", alert);
        } catch (Exception ex) {
            log.warn("Could not broadcast SOS WebSocket alert: {}", ex.getMessage());
        }
    }
}