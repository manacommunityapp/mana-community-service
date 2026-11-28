package com.manacommunity.api.tenancy.service;

import com.manacommunity.api.tenancy.entity.MoveOutSaga;
import com.manacommunity.api.tenancy.entity.MoveOutSaga.SagaStatus;
import com.manacommunity.api.tenancy.entity.MoveOutSaga.StepStatus;
import com.manacommunity.api.tenancy.entity.TenantRecord;
import com.manacommunity.api.tenancy.repository.MoveOutSagaRepository;
import com.manacommunity.api.tenancy.repository.TenantRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoveOutSagaService {

    private final MoveOutSagaRepository sagaRepository;
    private final TenantRecordRepository tenantRepository;

    @Transactional
    public MoveOutSaga initiateMoveOut(Long tenantRecordId) {
        TenantRecord tenant = tenantRepository.findById(tenantRecordId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantRecordId));

        MoveOutSaga saga = MoveOutSaga.builder()
                .tenantRecordId(tenantRecordId)
                .flatNumber(tenant.getFlatNumber())
                .towerName(tenant.getTowerName())
                .overallStatus(SagaStatus.INITIATED)
                .duesVerification(StepStatus.PENDING)
                .appAccessRevocation(StepStatus.PENDING)
                .rfidDeactivation(StepStatus.PENDING)
                .dailyHelpUnlinking(StepStatus.PENDING)
                .gatePassIssuance(StepStatus.PENDING)
                .auditPreservation(StepStatus.PENDING)
                .totalTags(tenant.getActiveRfidTags())
                .pendingAmount(tenant.getPendingDues())
                .build();

        tenant.setStatus(TenantRecord.TenantStatus.MOVE_OUT_INITIATED);
        tenantRepository.save(tenant);

        return sagaRepository.save(saga);
    }

    @Transactional
    public MoveOutSaga executeSaga(Long sagaId) {
        MoveOutSaga saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        saga.setOverallStatus(SagaStatus.IN_PROGRESS);
        List<String> compensationLog = new ArrayList<>();

        try {
            // Step 1: Verify zero pending dues
            executeDuesVerification(saga);

            // Step 2: Revoke app access
            executeAppAccessRevocation(saga);

            // Step 3: Deactivate RFID tags
            executeRfidDeactivation(saga);

            // Step 4: Unlink daily helps
            executeDailyHelpUnlinking(saga);

            // Step 5: Issue gate pass
            executeGatePassIssuance(saga);

            // Step 6: Preserve audit logs
            executeAuditPreservation(saga);

            saga.setOverallStatus(SagaStatus.COMPLETED);
            saga.setCompletedAt(LocalDateTime.now());

            TenantRecord tenant = tenantRepository.findById(saga.getTenantRecordId()).orElse(null);
            if (tenant != null) {
                tenant.setStatus(TenantRecord.TenantStatus.MOVED_OUT);
                tenant.setMoveOutDate(LocalDateTime.now().toLocalDate());
                tenant.setAppAccessActive(false);
                tenant.setActiveRfidTags(0);
                tenant.setLinkedDailyHelps(0);
                tenantRepository.save(tenant);
            }

            log.info("Move-out saga completed for flat {}-{}", saga.getTowerName(), saga.getFlatNumber());
        } catch (Exception e) {
            saga.setOverallStatus(SagaStatus.FAILED);
            saga.setFailureReason(e.getMessage());
            log.error("Saga failed at step: {}", saga.getFailedStep(), e);
        }

        return sagaRepository.save(saga);
    }

    private void executeDuesVerification(MoveOutSaga saga) {
        saga.setDuesVerification(StepStatus.IN_PROGRESS);
        saga.setDuesCheckedAt(LocalDateTime.now());

        if (saga.getPendingAmount() > 0) {
            saga.setDuesVerification(StepStatus.FAILED);
            saga.setFailedStep("DUES_VERIFICATION");
            saga.setFailureReason("Pending dues of ₹" + saga.getPendingAmount());
            throw new RuntimeException("Cannot proceed: pending dues of ₹" + saga.getPendingAmount());
        }

        saga.setDuesVerification(StepStatus.COMPLETED);
    }

    private void executeAppAccessRevocation(MoveOutSaga saga) {
        saga.setAppAccessRevocation(StepStatus.IN_PROGRESS);
        // Simulate: revoke OAuth tokens, invalidate sessions, disable user account
        saga.setAppAccessRevocation(StepStatus.COMPLETED);
        saga.setAccessRevokedAt(LocalDateTime.now());
    }

    private void executeRfidDeactivation(MoveOutSaga saga) {
        saga.setRfidDeactivation(StepStatus.IN_PROGRESS);
        // Simulate: send deactivation commands to each RFID reader for tenant's tags
        saga.setTagsDeactivated(saga.getTotalTags());
        saga.setRfidDeactivation(StepStatus.COMPLETED);
        saga.setRfidDeactivatedAt(LocalDateTime.now());
    }

    private void executeDailyHelpUnlinking(MoveOutSaga saga) {
        saga.setDailyHelpUnlinking(StepStatus.IN_PROGRESS);
        // Simulate: unlink maid, cook, driver entries from this flat
        TenantRecord tenant = tenantRepository.findById(saga.getTenantRecordId()).orElse(null);
        int helps = tenant != null ? tenant.getLinkedDailyHelps() : 0;
        saga.setHelpsUnlinked(helps);
        saga.setDailyHelpUnlinking(StepStatus.COMPLETED);
        saga.setHelpsUnlinkedAt(LocalDateTime.now());
    }

    private void executeGatePassIssuance(MoveOutSaga saga) {
        saga.setGatePassIssuance(StepStatus.IN_PROGRESS);
        String gatePass = "GP-MO-" + saga.getTenantRecordId() + "-" + System.currentTimeMillis() % 10000;
        saga.setGatePassNumber(gatePass);
        saga.setGatePassIssuance(StepStatus.COMPLETED);
        saga.setGatePassIssuedAt(LocalDateTime.now());
    }

    private void executeAuditPreservation(MoveOutSaga saga) {
        saga.setAuditPreservation(StepStatus.IN_PROGRESS);
        // Simulate: archive visitor logs, payment history, complaint history to owner's view
        int records = 42 + new Random().nextInt(100);
        saga.setAuditRecordsArchived(records);
        saga.setAuditPreservation(StepStatus.COMPLETED);
        saga.setAuditPreservedAt(LocalDateTime.now());
    }

    @Transactional
    public MoveOutSaga compensate(Long sagaId) {
        MoveOutSaga saga = sagaRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found"));

        saga.setOverallStatus(SagaStatus.COMPENSATING);
        StringBuilder log = new StringBuilder();

        if (saga.getAuditPreservation() == StepStatus.COMPLETED) {
            log.append("Audit logs restored from archive. ");
            saga.setAuditPreservation(StepStatus.COMPENSATED);
        }
        if (saga.getGatePassIssuance() == StepStatus.COMPLETED) {
            log.append("Gate pass ").append(saga.getGatePassNumber()).append(" revoked. ");
            saga.setGatePassIssuance(StepStatus.COMPENSATED);
        }
        if (saga.getDailyHelpUnlinking() == StepStatus.COMPLETED) {
            log.append(saga.getHelpsUnlinked()).append(" daily helps re-linked. ");
            saga.setDailyHelpUnlinking(StepStatus.COMPENSATED);
        }
        if (saga.getRfidDeactivation() == StepStatus.COMPLETED) {
            log.append(saga.getTagsDeactivated()).append(" RFID tags reactivated. ");
            saga.setRfidDeactivation(StepStatus.COMPENSATED);
        }
        if (saga.getAppAccessRevocation() == StepStatus.COMPLETED) {
            log.append("App access restored. ");
            saga.setAppAccessRevocation(StepStatus.COMPENSATED);
        }

        saga.setCompensationLog(log.toString());
        saga.setOverallStatus(SagaStatus.COMPENSATED);

        TenantRecord tenant = tenantRepository.findById(saga.getTenantRecordId()).orElse(null);
        if (tenant != null) {
            tenant.setStatus(TenantRecord.TenantStatus.ACTIVE);
            tenant.setAppAccessActive(true);
            tenantRepository.save(tenant);
        }

        return sagaRepository.save(saga);
    }

    public SagaSummary getSagaSummary(Long communityId) {
        List<MoveOutSaga> all = sagaRepository.findAll();
        long completed = all.stream().filter(s -> s.getOverallStatus() == SagaStatus.COMPLETED).count();
        long inProgress = all.stream().filter(s -> s.getOverallStatus() == SagaStatus.IN_PROGRESS || s.getOverallStatus() == SagaStatus.INITIATED).count();
        long failed = all.stream().filter(s -> s.getOverallStatus() == SagaStatus.FAILED).count();

        double avgSteps = all.stream()
                .filter(s -> s.getOverallStatus() == SagaStatus.COMPLETED)
                .mapToLong(s -> java.time.Duration.between(
                        s.getCreatedAt() != null ? s.getCreatedAt() : LocalDateTime.now(),
                        s.getCompletedAt() != null ? s.getCompletedAt() : LocalDateTime.now()).toMinutes())
                .average().orElse(0);

        return new SagaSummary(completed, inProgress, failed, avgSteps, all);
    }

    public record SagaSummary(long completed, long inProgress, long failed,
                               double avgCompletionMinutes, List<MoveOutSaga> sagas) {}
}
