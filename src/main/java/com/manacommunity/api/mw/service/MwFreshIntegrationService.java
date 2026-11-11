package com.manacommunity.api.mw.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.mw.dto.CompleteFreshDeliveryRequest;
import com.manacommunity.api.mw.dto.CreateFreshDeliveryRequest;
import com.manacommunity.api.mw.entity.MwFreshAssignment;
import com.manacommunity.api.mw.entity.MwIntegrationConfig;
import com.manacommunity.api.mw.entity.MwOperationalAssignment;
import com.manacommunity.api.mw.repository.MwFreshAssignmentRepository;
import com.manacommunity.api.mw.repository.MwIntegrationConfigRepository;
import com.manacommunity.api.mw.repository.MwOperationalAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MwFreshIntegrationService {

    private final MwFreshAssignmentRepository freshRepo;
    private final MwOperationalAssignmentRepository opRepo;
    private final MwIntegrationConfigRepository configRepo;

    public MwFreshAssignment createDeliveryAssignment(Long orgId, CreateFreshDeliveryRequest req) {
        // Idempotent — same freshOrderId returns existing
        Optional<MwFreshAssignment> existing = freshRepo.findByFreshOrderId(req.getFreshOrderId());
        if (existing.isPresent()) return existing.get();

        MwIntegrationConfig config = configRepo.findByOrganizationId(orgId)
                .orElseThrow(() -> new IllegalStateException("Fresh integration not configured for org"));
        if (!Boolean.TRUE.equals(config.getFreshEnabled())) {
            throw new IllegalStateException("Fresh integration is not enabled for this org");
        }

        BigDecimal basePay = config.getBaseDeliveryPay() != null ? config.getBaseDeliveryPay() : BigDecimal.ZERO;

        // Derive a numeric sourceRefId from the order id string
        String digitsOnly = req.getFreshOrderId().replaceAll("[^0-9]", "");
        Long sourceRefId = digitsOnly.isEmpty() ? 0L
                : Long.parseLong(digitsOnly.substring(0, Math.min(18, digitsOnly.length())));

        MwOperationalAssignment opAssignment = opRepo.save(MwOperationalAssignment.builder()
                .organizationId(orgId)
                .sourceModule("MANA_FRESH")
                .sourceRefId(sourceRefId)
                .employeeId(req.getEmployeeId())
                .assignmentType(req.getAssignmentType() != null ? req.getAssignmentType() : "DELIVERY")
                .incentiveAmount(basePay)
                .build());

        return freshRepo.save(MwFreshAssignment.builder()
                .operationalAssignment(opAssignment)
                .freshOrderId(req.getFreshOrderId())
                .pickupLocation(req.getPickupLocation())
                .dropLocation(req.getDropLocation())
                .estimatedKm(req.getEstimatedKm())
                .basePay(basePay)
                .build());
    }

    public MwFreshAssignment completeDeliveryAssignment(Long orgId, Long freshAssignmentId, CompleteFreshDeliveryRequest req) {
        MwFreshAssignment fa = freshRepo.findById(freshAssignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fresh assignment not found"));
        if (!fa.getOperationalAssignment().getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Not in your org");
        }

        MwIntegrationConfig config = configRepo.findByOrganizationId(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Integration config not found"));

        fa.setActualKm(req.getActualKm());
        fa.setOnTime(req.getOnTime());

        if (req.getActualKm() != null && config.getPerKmRate() != null) {
            fa.setKmBonus(req.getActualKm().multiply(config.getPerKmRate()));
        }
        if (Boolean.TRUE.equals(req.getOnTime()) && config.getOnTimeBonus() != null) {
            fa.setTimeBonus(config.getOnTimeBonus());
        }

        MwOperationalAssignment op = fa.getOperationalAssignment();
        op.setStatus("COMPLETED");
        op.setCompletedAt(LocalDateTime.now());
        if (req.getPerformanceScore() != null) op.setPerformanceScore(req.getPerformanceScore());
        BigDecimal total = fa.getBasePay()
                .add(fa.getKmBonus() != null ? fa.getKmBonus() : BigDecimal.ZERO)
                .add(fa.getTimeBonus() != null ? fa.getTimeBonus() : BigDecimal.ZERO);
        op.setIncentiveAmount(total);
        opRepo.save(op);

        return freshRepo.save(fa);
    }

    @Transactional(readOnly = true)
    public List<MwFreshAssignment> getOrgDeliveries(Long orgId) {
        return freshRepo.findByOrganizationId(orgId);
    }

    @Transactional(readOnly = true)
    public List<MwFreshAssignment> getEmployeeDeliveries(Long employeeId) {
        return freshRepo.findByEmployeeId(employeeId);
    }
}
