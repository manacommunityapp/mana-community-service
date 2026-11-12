package com.manacommunity.api.mw.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.mw.dto.CreateHandoverRequest;
import com.manacommunity.api.mw.entity.MwDutyHandover;
import com.manacommunity.api.mw.repository.MwDutyHandoverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MwHandoverService {

    private final MwDutyHandoverRepository handoverRepo;

    public MwDutyHandover createHandover(Long orgId, CreateHandoverRequest req) {
        return handoverRepo.save(MwDutyHandover.builder()
                .organizationId(orgId)
                .shiftAssignmentId(req.getShiftAssignmentId())
                .handoverFromId(req.getHandoverFromEmployeeId())
                .handoverToId(req.getHandoverToEmployeeId())
                .location(req.getLocation())
                .notes(req.getNotes())
                .pendingTasks(req.getPendingTasks())
                .equipmentStatus(req.getEquipmentStatus())
                .incidentFlag(req.getIncidentFlag())
                .incidentDesc(req.getIncidentDesc())
                .build());
    }

    public MwDutyHandover acknowledgeHandover(Long orgId, Long handoverId) {
        MwDutyHandover handover = handoverRepo.findById(handoverId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover not found"));
        if (!handover.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Not in your org");
        }
        handover.setAcknowledged(true);
        handover.setAcknowledgedAt(LocalDateTime.now());
        return handoverRepo.save(handover);
    }

    public List<MwDutyHandover> getHandovers(Long orgId, LocalDate date) {
        if (date != null) {
            return handoverRepo.findByOrganizationIdAndDate(orgId, date);
        }
        return handoverRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    public List<MwDutyHandover> getPendingAcknowledgements(Long orgId, Long handoverToId) {
        return handoverRepo.findByOrganizationIdAndHandoverToIdAndAcknowledgedFalse(orgId, handoverToId);
    }
}
