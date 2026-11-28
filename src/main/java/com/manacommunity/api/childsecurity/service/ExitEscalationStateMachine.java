package com.manacommunity.api.childsecurity.service;

import com.manacommunity.api.childsecurity.entity.*;
import com.manacommunity.api.childsecurity.entity.ChildExitRequest.*;
import com.manacommunity.api.childsecurity.entity.EscalationLog.*;
import com.manacommunity.api.childsecurity.entity.GuardianAuthorization.GuardianRole;
import com.manacommunity.api.childsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExitEscalationStateMachine {

    private final ChildExitRequestRepository exitRepo;
    private final EscalationLogRepository escalationRepo;
    private final GuardianAuthorizationRepository guardianRepo;
    private final ChildProfileRepository childRepo;

    private static final int PRIMARY_TIMEOUT_SECONDS = 60;
    private static final int BACKUP_TIMEOUT_SECONDS = 60;
    private static final int MAX_ESCALATION_ATTEMPTS = 4;

    @Transactional
    public ChildExitRequest initiateExitRequest(Long childId, String guardOnDuty, String gateId,
                                                  ExitType exitType, String companionName,
                                                  String companionPhone, String companionRelation) {
        ChildProfile child = childRepo.findById(childId)
                .orElseThrow(() -> new RuntimeException("Child not found"));

        if (!child.isExitLockEnabled() && exitType != ExitType.UNACCOMPANIED) {
            return createAutoApproved(child, guardOnDuty, gateId, exitType);
        }

        List<GuardianAuthorization> guardians = guardianRepo.findByChildIdOrderByEscalation(childId);
        if (guardians.isEmpty()) throw new RuntimeException("No guardians registered for child");

        ChildExitRequest request = ChildExitRequest.builder()
                .childId(childId).childName(child.getChildName())
                .communityId(child.getCommunityId())
                .flatNumber(child.getFlatNumber()).towerName(child.getTowerName())
                .exitType(exitType).status(ExitRequestStatus.PENDING)
                .escalationState(EscalationState.AWAITING_PRIMARY)
                .guardOnDuty(guardOnDuty).gateId(gateId)
                .requestedAt(LocalDateTime.now())
                .companionName(companionName).companionPhone(companionPhone)
                .companionRelation(companionRelation)
                .escalationTimeoutSeconds(PRIMARY_TIMEOUT_SECONDS)
                .totalEscalationAttempts(0)
                .build();

        request = exitRepo.save(request);

        GuardianAuthorization primary = guardians.stream()
                .filter(g -> g.getRole() == GuardianRole.PRIMARY_PARENT)
                .findFirst().orElse(guardians.get(0));

        notifyGuardian(request, primary, EscalationMethod.PUSH_NOTIFICATION, 1);
        request.setPrimaryNotifiedAt(LocalDateTime.now());

        return exitRepo.save(request);
    }

    @Transactional
    public ChildExitRequest processTimeout(Long requestId) {
        ChildExitRequest request = exitRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != ExitRequestStatus.PENDING) return request;

        List<GuardianAuthorization> guardians = guardianRepo.findByChildIdOrderByEscalation(request.getChildId());

        switch (request.getEscalationState()) {
            case AWAITING_PRIMARY -> {
                request.setEscalationState(EscalationState.PRIMARY_TIMEOUT_ESCALATE_BACKUP);
                request.setEscalatedAt(LocalDateTime.now());

                GuardianAuthorization backup = guardians.stream()
                        .filter(g -> g.getRole() == GuardianRole.SECONDARY_PARENT)
                        .findFirst()
                        .orElse(guardians.size() > 1 ? guardians.get(1) : null);

                if (backup != null) {
                    notifyGuardian(request, backup, EscalationMethod.PUSH_NOTIFICATION, 2);
                    notifyGuardian(request, backup, EscalationMethod.SMS, 2);
                } else {
                    request.setEscalationState(EscalationState.IVR_TRIGGERED);
                    triggerIvr(request, guardians.get(0));
                }
            }
            case PRIMARY_TIMEOUT_ESCALATE_BACKUP -> {
                request.setEscalationState(EscalationState.IVR_TRIGGERED);
                GuardianAuthorization primaryForIvr = guardians.stream()
                        .filter(GuardianAuthorization::isIvrEnabled)
                        .findFirst().orElse(guardians.get(0));
                triggerIvr(request, primaryForIvr);
            }
            case IVR_TRIGGERED -> {
                if (request.getTotalEscalationAttempts() >= MAX_ESCALATION_ATTEMPTS) {
                    request.setEscalationState(EscalationState.ALL_ESCALATIONS_EXHAUSTED);
                    request.setStatus(ExitRequestStatus.AUTO_LOCKED);
                    request.setRemarks("All escalation attempts exhausted. Child held at gate.");
                } else {
                    GuardianAuthorization emergency = guardians.stream()
                            .filter(g -> g.getRole() == GuardianRole.EMERGENCY_CONTACT)
                            .findFirst().orElse(null);
                    if (emergency != null) {
                        notifyGuardian(request, emergency, EscalationMethod.IVR_CALL, request.getTotalEscalationAttempts() + 1);
                    } else {
                        request.setEscalationState(EscalationState.AUTO_DENY);
                        request.setStatus(ExitRequestStatus.AUTO_LOCKED);
                    }
                }
            }
            default -> {}
        }

        return exitRepo.save(request);
    }

    @Transactional
    public ChildExitRequest guardianRespond(Long requestId, Long guardianId, boolean approved) {
        ChildExitRequest request = exitRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        GuardianAuthorization guardian = guardianRepo.findById(guardianId)
                .orElseThrow(() -> new RuntimeException("Guardian not found"));

        request.setEscalationState(EscalationState.GUARDIAN_RESPONDED);

        if (approved) {
            request.setStatus(ExitRequestStatus.APPROVED);
            request.setApprovedAt(LocalDateTime.now());
            request.setApprovedByGuardianId(guardianId);
            request.setApprovedByName(guardian.getGuardianName());
            request.setApprovalMethod(guardian.getRole().name());
        } else {
            request.setStatus(ExitRequestStatus.DENIED);
            request.setDeniedAt(LocalDateTime.now());
            request.setRemarks("Denied by " + guardian.getGuardianName());
        }

        List<EscalationLog> logs = escalationRepo.findByExitRequestIdOrderByAttemptNumber(requestId);
        logs.stream()
                .filter(l -> l.getGuardianId().equals(guardianId) && l.getOutcome() == EscalationOutcome.PENDING)
                .findFirst().ifPresent(l -> {
                    l.setOutcome(approved ? EscalationOutcome.APPROVED : EscalationOutcome.DENIED);
                    l.setRespondedAt(LocalDateTime.now());
                    l.setResponseTimeSeconds((int) ChronoUnit.SECONDS.between(l.getInitiatedAt(), LocalDateTime.now()));
                    escalationRepo.save(l);
                });

        return exitRepo.save(request);
    }

    private void notifyGuardian(ChildExitRequest request, GuardianAuthorization guardian,
                                  EscalationMethod method, int attempt) {
        escalationRepo.save(EscalationLog.builder()
                .exitRequestId(request.getId())
                .guardianId(guardian.getId())
                .guardianName(guardian.getGuardianName())
                .guardianPhone(guardian.getGuardianPhone())
                .method(method).outcome(EscalationOutcome.PENDING)
                .initiatedAt(LocalDateTime.now())
                .attemptNumber(attempt).build());

        request.setTotalEscalationAttempts(attempt);
    }

    private void triggerIvr(ChildExitRequest request, GuardianAuthorization guardian) {
        request.setIvrTriggeredAt(LocalDateTime.now());
        String callSid = "IVR-" + UUID.randomUUID().toString().substring(0, 8);

        escalationRepo.save(EscalationLog.builder()
                .exitRequestId(request.getId())
                .guardianId(guardian.getId())
                .guardianName(guardian.getGuardianName())
                .guardianPhone(guardian.getGuardianPhone())
                .method(EscalationMethod.IVR_CALL).outcome(EscalationOutcome.PENDING)
                .initiatedAt(LocalDateTime.now())
                .attemptNumber(request.getTotalEscalationAttempts() + 1)
                .ivrCallSid(callSid).build());

        request.setTotalEscalationAttempts(request.getTotalEscalationAttempts() + 1);
    }

    private ChildExitRequest createAutoApproved(ChildProfile child, String guard, String gate, ExitType type) {
        return exitRepo.save(ChildExitRequest.builder()
                .childId(child.getId()).childName(child.getChildName())
                .communityId(child.getCommunityId())
                .flatNumber(child.getFlatNumber()).towerName(child.getTowerName())
                .exitType(type).status(ExitRequestStatus.APPROVED)
                .escalationState(EscalationState.GUARDIAN_RESPONDED)
                .guardOnDuty(guard).gateId(gate)
                .requestedAt(LocalDateTime.now()).approvedAt(LocalDateTime.now())
                .approvalMethod("EXIT_LOCK_DISABLED")
                .escalationTimeoutSeconds(0).totalEscalationAttempts(0)
                .build());
    }

    public ExitSecurityDashboard getDashboard() {
        List<ChildProfile> children = childRepo.findAll();
        List<ChildExitRequest> requests = exitRepo.findAll();
        List<EscalationLog> logs = escalationRepo.findAll();
        List<GuardianAuthorization> guardians = guardianRepo.findAll();

        long exitLocked = children.stream().filter(ChildProfile::isExitLockEnabled).count();
        long pendingRequests = requests.stream().filter(r -> r.getStatus() == ExitRequestStatus.PENDING).count();
        long approvedToday = requests.stream().filter(r -> r.getStatus() == ExitRequestStatus.APPROVED).count();
        long deniedTotal = requests.stream().filter(r -> r.getStatus() == ExitRequestStatus.DENIED || r.getStatus() == ExitRequestStatus.AUTO_LOCKED).count();

        double avgResponseTime = logs.stream()
                .filter(l -> l.getResponseTimeSeconds() > 0)
                .mapToInt(EscalationLog::getResponseTimeSeconds)
                .average().orElse(0);

        Map<String, Long> requestsByStatus = requests.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));

        Map<String, Long> escalationsByMethod = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getMethod().name(), Collectors.counting()));

        return new ExitSecurityDashboard(children.size(), exitLocked, pendingRequests,
                approvedToday, deniedTotal, Math.round(avgResponseTime),
                requestsByStatus, escalationsByMethod,
                children, requests, guardians, logs);
    }

    public record ExitSecurityDashboard(int totalChildren, long exitLockedChildren, long pendingRequests,
                                          long approvedRequests, long deniedRequests, long avgResponseTimeSeconds,
                                          Map<String, Long> requestsByStatus, Map<String, Long> escalationsByMethod,
                                          List<ChildProfile> children, List<ChildExitRequest> exitRequests,
                                          List<GuardianAuthorization> guardians, List<EscalationLog> escalationLogs) {}
}
