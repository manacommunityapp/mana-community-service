package com.manacommunity.api.childsecurity;

import com.manacommunity.api.childsecurity.entity.*;
import com.manacommunity.api.childsecurity.entity.ChildExitRequest.*;
import com.manacommunity.api.childsecurity.entity.EscalationLog.*;
import com.manacommunity.api.childsecurity.entity.GuardianAuthorization.*;
import com.manacommunity.api.childsecurity.repository.*;
import com.manacommunity.api.childsecurity.service.ExitEscalationStateMachine;
import com.manacommunity.api.exception.UnauthorizedActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExitEscalationStateMachine's Primary-Parent-Only guardrail logic.
 *
 * <p>Tests cover:
 * <ol>
 *   <li>PRIMARY_PARENT can approve when exitLockEnabled=true → APPROVED</li>
 *   <li>SECONDARY_PARENT approving when exitLockEnabled=true → UnauthorizedActionException</li>
 *   <li>VIEW_ONLY guardian approving → UnauthorizedActionException</li>
 *   <li>60-second timeout escalates AWAITING_PRIMARY → PRIMARY_TIMEOUT_ESCALATE_BACKUP with IVR log</li>
 *   <li>All escalation attempts exhausted → AUTO_LOCKED</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class ChildExitGuardrailTest {

    @Mock ChildExitRequestRepository exitRepo;
    @Mock EscalationLogRepository escalationRepo;
    @Mock GuardianAuthorizationRepository guardianRepo;
    @Mock ChildProfileRepository childRepo;

    @InjectMocks ExitEscalationStateMachine stateMachine;

    // ── Shared fixtures ──────────────────────────────────────────────────────

    private static final Long CHILD_ID     = 1L;
    private static final Long REQUEST_ID   = 10L;
    private static final Long PRIMARY_ID   = 100L;
    private static final Long SECONDARY_ID = 101L;
    private static final Long VIEW_ONLY_ID = 102L;

    private ChildProfile lockedChild;
    private GuardianAuthorization primaryGuardian;
    private GuardianAuthorization secondaryGuardian;
    private GuardianAuthorization viewOnlyGuardian;
    private ChildExitRequest pendingRequest;

    @BeforeEach
    void setUp() {
        lockedChild = ChildProfile.builder()
                .id(CHILD_ID)
                .communityId("COMM-1")
                .childName("Arjun Kumar")
                .flatNumber("A-204")
                .towerName("Tower-A")
                .exitLockEnabled(true)
                .status(ChildProfile.ChildStatus.ACTIVE)
                .dateOfBirth(LocalDate.of(2018, 5, 10))
                .age(8)
                .build();

        primaryGuardian = GuardianAuthorization.builder()
                .id(PRIMARY_ID)
                .childId(CHILD_ID)
                .guardianName("Ravi Kumar")
                .guardianPhone("9000000001")
                .role(GuardianRole.PRIMARY_PARENT)
                .authLevel(AuthorizationLevel.FULL_APPROVE)
                .escalationOrder(1)
                .isActive(true)
                .ivrEnabled(true)
                .pushNotificationEnabled(true)
                .build();

        secondaryGuardian = GuardianAuthorization.builder()
                .id(SECONDARY_ID)
                .childId(CHILD_ID)
                .guardianName("Priya Kumar")
                .guardianPhone("9000000002")
                .role(GuardianRole.SECONDARY_PARENT)
                .authLevel(AuthorizationLevel.FULL_APPROVE)
                .escalationOrder(2)
                .isActive(true)
                .ivrEnabled(false)
                .pushNotificationEnabled(true)
                .build();

        viewOnlyGuardian = GuardianAuthorization.builder()
                .id(VIEW_ONLY_ID)
                .childId(CHILD_ID)
                .guardianName("Grandma Devi")
                .guardianPhone("9000000003")
                .role(GuardianRole.GRANDPARENT)
                .authLevel(AuthorizationLevel.VIEW_ONLY)
                .escalationOrder(3)
                .isActive(true)
                .ivrEnabled(false)
                .pushNotificationEnabled(false)
                .build();

        pendingRequest = ChildExitRequest.builder()
                .id(REQUEST_ID)
                .childId(CHILD_ID)
                .childName("Arjun Kumar")
                .communityId("COMM-1")
                .flatNumber("A-204")
                .towerName("Tower-A")
                .exitType(ExitType.WITH_GUARDIAN)
                .status(ExitRequestStatus.PENDING)
                .escalationState(EscalationState.AWAITING_PRIMARY)
                .guardOnDuty("GUARD-01")
                .gateId("GATE-MAIN")
                .requestedAt(LocalDateTime.now().minusSeconds(30))
                .primaryNotifiedAt(LocalDateTime.now().minusSeconds(30))
                .escalationTimeoutSeconds(60)
                .totalEscalationAttempts(1)
                .build();
    }

    // ── Helper: make exitRepo.save() return whatever was passed in ───────────
    private void stubSavePassThrough() {
        when(exitRepo.save(any(ChildExitRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 1 — PRIMARY_PARENT approves when exitLockEnabled=true → APPROVED
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("PRIMARY_PARENT approves exit lock request → status=APPROVED")
    void primaryParent_canApprove_whenExitLockEnabled() {
        stubSavePassThrough();
        when(exitRepo.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));
        when(guardianRepo.findById(PRIMARY_ID)).thenReturn(Optional.of(primaryGuardian));
        when(childRepo.findById(CHILD_ID)).thenReturn(Optional.of(lockedChild));
        when(escalationRepo.findByExitRequestIdOrderByAttemptNumber(REQUEST_ID))
                .thenReturn(List.of());  // no prior escalation logs to update

        ChildExitRequest result = stateMachine.guardianRespond(REQUEST_ID, PRIMARY_ID, true);

        assertThat(result.getStatus()).isEqualTo(ExitRequestStatus.APPROVED);
        assertThat(result.getApprovedByGuardianId()).isEqualTo(PRIMARY_ID);
        assertThat(result.getApprovedByName()).isEqualTo("Ravi Kumar");
        assertThat(result.getApprovalMethod()).isEqualTo(GuardianRole.PRIMARY_PARENT.name());
        assertThat(result.getEscalationState()).isEqualTo(EscalationState.GUARDIAN_RESPONDED);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 2 — SECONDARY_PARENT approve when exitLockEnabled=true → exception
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("SECONDARY_PARENT approve when exit lock active → UnauthorizedActionException")
    void secondaryParent_blocked_whenExitLockEnabled() {
        when(exitRepo.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));
        when(guardianRepo.findById(SECONDARY_ID)).thenReturn(Optional.of(secondaryGuardian));
        when(childRepo.findById(CHILD_ID)).thenReturn(Optional.of(lockedChild));

        assertThatThrownBy(() -> stateMachine.guardianRespond(REQUEST_ID, SECONDARY_ID, true))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("Primary Parent Only exit lock active");

        // Ensure the request was NOT saved as APPROVED
        verify(exitRepo, never()).save(argThat(r -> r.getStatus() == ExitRequestStatus.APPROVED));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 3 — VIEW_ONLY guardian cannot approve regardless of lock state
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("VIEW_ONLY guardian approve attempt → UnauthorizedActionException (checked before lock)")
    void viewOnly_cannotApprove_regardlessOfLockState() {
        when(exitRepo.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));
        when(guardianRepo.findById(VIEW_ONLY_ID)).thenReturn(Optional.of(viewOnlyGuardian));

        assertThatThrownBy(() -> stateMachine.guardianRespond(REQUEST_ID, VIEW_ONLY_ID, true))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("VIEW_ONLY guardian cannot approve");

        // VIEW_ONLY check fires BEFORE the lock check → childRepo should not even be queried
        verify(childRepo, never()).findById(any());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 4 — 60-second timeout: AWAITING_PRIMARY → PRIMARY_TIMEOUT_ESCALATE_BACKUP
    //          and backup guardian receives PUSH + SMS notifications
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("processTimeout on AWAITING_PRIMARY → escalates to backup, logs PUSH+SMS")
    void timeout_awaitingPrimary_escalatesToBackupWithPushAndSms() {
        stubSavePassThrough();
        when(exitRepo.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));
        when(guardianRepo.findByChildIdOrderByEscalation(CHILD_ID))
                .thenReturn(List.of(primaryGuardian, secondaryGuardian));

        ArgumentCaptor<EscalationLog> logCaptor = ArgumentCaptor.forClass(EscalationLog.class);
        when(escalationRepo.save(logCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        ChildExitRequest result = stateMachine.processTimeout(REQUEST_ID);

        assertThat(result.getEscalationState())
                .isEqualTo(EscalationState.PRIMARY_TIMEOUT_ESCALATE_BACKUP);
        assertThat(result.getEscalatedAt()).isNotNull();

        List<EscalationLog> logs = logCaptor.getAllValues();
        assertThat(logs).hasSize(2); // PUSH + SMS for backup
        assertThat(logs.get(0).getMethod()).isEqualTo(EscalationMethod.PUSH_NOTIFICATION);
        assertThat(logs.get(1).getMethod()).isEqualTo(EscalationMethod.SMS);
        logs.forEach(l -> {
            assertThat(l.getGuardianId()).isEqualTo(SECONDARY_ID);
            assertThat(l.getOutcome()).isEqualTo(EscalationOutcome.PENDING);
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 5 — All escalation attempts exhausted → AUTO_LOCKED
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("processTimeout when MAX_ESCALATION_ATTEMPTS reached → AUTO_LOCKED")
    void timeout_allEscalationsExhausted_autoLocks() {
        stubSavePassThrough();

        // Put request in IVR_TRIGGERED state with 4 attempts already consumed
        pendingRequest.setEscalationState(EscalationState.IVR_TRIGGERED);
        pendingRequest.setTotalEscalationAttempts(4); // MAX_ESCALATION_ATTEMPTS = 4

        when(exitRepo.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));
        when(guardianRepo.findByChildIdOrderByEscalation(CHILD_ID))
                .thenReturn(List.of(primaryGuardian, secondaryGuardian));

        ChildExitRequest result = stateMachine.processTimeout(REQUEST_ID);

        assertThat(result.getStatus()).isEqualTo(ExitRequestStatus.AUTO_LOCKED);
        assertThat(result.getEscalationState()).isEqualTo(EscalationState.ALL_ESCALATIONS_EXHAUSTED);
        assertThat(result.getRemarks()).contains("exhausted");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Test 6 — SECONDARY_PARENT can DENY even when exit lock is active
    //          (lock only blocks approval, not denial)
    // ────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("SECONDARY_PARENT can DENY request even when exit lock active")
    void secondaryParent_canDeny_whenExitLockEnabled() {
        stubSavePassThrough();
        when(exitRepo.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));
        when(guardianRepo.findById(SECONDARY_ID)).thenReturn(Optional.of(secondaryGuardian));
        when(escalationRepo.findByExitRequestIdOrderByAttemptNumber(REQUEST_ID))
                .thenReturn(List.of());

        ChildExitRequest result = stateMachine.guardianRespond(REQUEST_ID, SECONDARY_ID, false);

        assertThat(result.getStatus()).isEqualTo(ExitRequestStatus.DENIED);
        assertThat(result.getDeniedAt()).isNotNull();
        assertThat(result.getRemarks()).contains("Priya Kumar");
        // childRepo.findById should never be called — the lock check is only on approved=true
        verify(childRepo, never()).findById(any());
    }
}
