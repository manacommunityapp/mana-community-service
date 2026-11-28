package com.manacommunity.api.unit.service;

import com.manacommunity.api.dailyhelp.entity.FlatStaffContract;
import com.manacommunity.api.dailyhelp.repository.DailyHelpRepositories.FlatStaffContractRepository;
import com.manacommunity.api.recovery.entity.RfidTagSnapshotEntity;
import com.manacommunity.api.recovery.repository.RfidTagSnapshotRepository;
import com.manacommunity.api.tenancy.entity.MoveOutSaga;
import com.manacommunity.api.tenancy.entity.MoveOutSaga.SagaStatus;
import com.manacommunity.api.tenancy.entity.MoveSlot;
import com.manacommunity.api.tenancy.entity.MoveSlot.SlotType;
import com.manacommunity.api.tenancy.entity.TenantDocument;
import com.manacommunity.api.tenancy.entity.TenantDocument.DocumentType;
import com.manacommunity.api.tenancy.entity.TenantDocument.VerificationStatus;
import com.manacommunity.api.tenancy.entity.TenantRecord;
import com.manacommunity.api.tenancy.repository.MoveOutSagaRepository;
import com.manacommunity.api.tenancy.repository.MoveSlotRepository;
import com.manacommunity.api.tenancy.repository.TenantDocumentRepository;
import com.manacommunity.api.tenancy.repository.TenantRecordRepository;
import com.manacommunity.api.tenancy.service.DocumentVerificationService;
import com.manacommunity.api.tenancy.service.MoveOutSagaService;
import com.manacommunity.api.tenancy.service.MoveSlotThrottlingService;
import com.manacommunity.api.visitor.entity.VisitorPass;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tenant Move-In, Move-Out Atomic Revocation, KYC Privacy & Slot Quota Tests")
class TenancyLifecycleAndKycTest {

    @Mock private MoveOutSagaRepository sagaRepository;
    @Mock private TenantRecordRepository tenantRepository;
    @Mock private VisitorPassRepository visitorPassRepository;
    @Mock private RfidTagSnapshotRepository rfidTagSnapshotRepository;
    @Mock private FlatStaffContractRepository flatStaffContractRepository;

    @Mock private TenantDocumentRepository documentRepository;
    @Mock private MoveSlotRepository moveSlotRepository;

    private MoveOutSagaService moveOutSagaService;
    private DocumentVerificationService documentVerificationService;
    private MoveSlotThrottlingService moveSlotThrottlingService;

    @BeforeEach
    void setUp() {
        moveOutSagaService = new MoveOutSagaService(sagaRepository, tenantRepository);
        moveOutSagaService.setVisitorPassRepository(visitorPassRepository);
        moveOutSagaService.setRfidTagSnapshotRepository(rfidTagSnapshotRepository);
        moveOutSagaService.setFlatStaffContractRepository(flatStaffContractRepository);

        documentVerificationService = new DocumentVerificationService(documentRepository, tenantRepository);
        moveSlotThrottlingService = new MoveSlotThrottlingService(moveSlotRepository);
    }

    @Test
    @DisplayName("Blocks move-out gate pass on unpaid dues and atomically revokes app, guest OTPs, RFID & daily help on clear")
    void shouldBlockUnpaidMoveOutAndAtomicallyRevokeAccessOnClearance() {
        TenantRecord tenantWithDues = TenantRecord.builder()
                .id(11L).communityId(1L).towerName("Tower A").flatNumber("A-402")
                .appAccessActive(true).activeRfidTags(2).linkedDailyHelps(2)
                .pendingDues(3500.0).status(TenantRecord.TenantStatus.ACTIVE)
                .build();

        when(tenantRepository.findById(11L)).thenReturn(Optional.of(tenantWithDues));
        when(tenantRepository.save(any(TenantRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sagaRepository.save(any(MoveOutSaga.class))).thenAnswer(inv -> {
            MoveOutSaga s = inv.getArgument(0);
            if (s.getId() == null) s.setId(500L);
            return s;
        });

        MoveOutSaga blockedSaga = moveOutSagaService.initiateMoveOut(11L);
        when(sagaRepository.findById(500L)).thenReturn(Optional.of(blockedSaga));
        MoveOutSaga failed = moveOutSagaService.executeSaga(500L);
        assertThat(failed.getOverallStatus()).isEqualTo(SagaStatus.FAILED);
        assertThat(failed.getFailedStep()).isEqualTo("DUES_VERIFICATION");
        assertThat(failed.getGatePassNumber()).isNull();
        assertThat(tenantWithDues.isAppAccessActive()).isTrue();

        // Settle dues and retry
        tenantWithDues.setPendingDues(0.0);
        VisitorPass guestPass = VisitorPass.builder()
                .id(91L).passCode("GP-9911").flatNumber("A-402").status(VisitorPass.PassStatus.APPROVED).build();
        RfidTagSnapshotEntity rfid1 = new RfidTagSnapshotEntity();
        rfid1.setTagId("RFID-A402-1");
        rfid1.setSocietyId(1L);
        rfid1.setFlatUnitCode("A-402");
        rfid1.setTagStatus("ACTIVE");
        FlatStaffContract maidContract = FlatStaffContract.builder().id(701L).flatId(11L).isActive(true).build();

        when(visitorPassRepository.findByCommunityAndStatuses(eq(1L), anyList())).thenReturn(List.of(guestPass));
        when(rfidTagSnapshotRepository.findBySocietyIdOrderByTagIdAsc(1L)).thenReturn(List.of(rfid1));
        when(flatStaffContractRepository.findByCommunityIdAndFlatIdAndIsActiveTrue(1L, 11L)).thenReturn(List.of(maidContract));

        MoveOutSaga clearSaga = moveOutSagaService.initiateMoveOut(11L);
        clearSaga.setId(501L);
        when(sagaRepository.findById(501L)).thenReturn(Optional.of(clearSaga));

        MoveOutSaga completed = moveOutSagaService.executeSaga(501L);
        assertThat(completed.getOverallStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(completed.getGatePassNumber()).startsWith("GP-MO-11-");
        assertThat(completed.getGuestPassesRevoked()).isEqualTo(1);
        assertThat(completed.getTagsDeactivated()).isEqualTo(1);
        assertThat(completed.getHelpsUnlinked()).isEqualTo(1);
        assertThat(guestPass.getStatus()).isEqualTo(VisitorPass.PassStatus.CANCELLED);
        assertThat(rfid1.getTagStatus()).isEqualTo("REVOKED_ON_MOVEOUT");
        assertThat(rfid1.getBlacklisted()).isTrue();
        assertThat(maidContract.getIsActive()).isFalse();
        assertThat(tenantWithDues.isAppAccessActive()).isFalse();
    }

    @Test
    @DisplayName("Validates KYC file size/extension limits, generates MC watermark, and sets 11-month lease expiry + reminder")
    void shouldEnforceKycFileLimitsWatermarkingAnd11MonthLeaseExpiry() {
        assertThatThrownBy(() -> documentVerificationService.uploadDocument(
                11L, DocumentType.AADHAAR, "oversize.pdf", 12L * 1024 * 1024))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 MB");

        assertThatThrownBy(() -> documentVerificationService.uploadDocument(
                11L, DocumentType.AADHAAR, "payload.exe", 50_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported KYC");

        when(documentRepository.save(any(TenantDocument.class))).thenAnswer(inv -> {
            TenantDocument d = inv.getArgument(0);
            if (d.getId() == null) d.setId(88L);
            return d;
        });

        TenantDocument leaseDoc = documentVerificationService.uploadDocument(
                11L, DocumentType.RENTAL_AGREEMENT, "signed_lease.pdf", 1024L * 512);

        assertThat(leaseDoc.getVerificationStatus()).isEqualTo(VerificationStatus.MC_REVIEW_PENDING);
        assertThat(leaseDoc.getEncryptionKeyRef()).startsWith("AES-256-KMS-");
        assertThat(leaseDoc.getWatermarkedPreviewPath()).contains("MC-CONFIDENTIAL");
        assertThat(leaseDoc.getDocumentExpiryDate()).isEqualTo(LocalDate.now().plusMonths(11));
        assertThat(leaseDoc.getExpiryReminderDate()).isEqualTo(LocalDate.now().plusMonths(10));
    }

    @Test
    @DisplayName("Throttles tower move-ins to max 2/day within 10:00-17:00 freight window and rejects colliding elevators")
    void shouldEnforceMoveSlotQuotasFreightHoursAndElevatorCollisions() {
        LocalDate moveDay = LocalDate.now().plusDays(3);

        // 08:00 AM outside 10:00-17:00 window rejected
        when(moveSlotRepository.findActiveSlotsByTowerAndDate("Tower A", moveDay, SlotType.MOVE_IN))
                .thenReturn(List.of());
        assertThatThrownBy(() -> moveSlotThrottlingService.bookSlot(
                1L, "Tower A", "A-101", 11L, moveDay, LocalTime.of(8, 0),
                SlotType.MOVE_IN, "TS09AA1111", "TRUCK", "Raju", "9999999999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("10:00");

        // Overlap on both service elevators rejected cleanly
        MoveSlot existingSlot = MoveSlot.builder()
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0)).build();
        when(moveSlotRepository.findElevatorBookings("Tower A-SVC-1", moveDay)).thenReturn(List.of(existingSlot));
        when(moveSlotRepository.findElevatorBookings("Tower A-SVC-2", moveDay)).thenReturn(List.of(existingSlot));

        assertThatThrownBy(() -> moveSlotThrottlingService.bookSlot(
                1L, "Tower A", "A-101", 11L, moveDay, LocalTime.of(10, 0),
                SlotType.MOVE_IN, "TS09AA1111", "TRUCK", "Raju", "9999999999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("All freight elevators");

        // Tower already at max 2/day quota rejected
        when(moveSlotRepository.findActiveSlotsByTowerAndDate("Tower A", moveDay, SlotType.MOVE_IN))
                .thenReturn(List.of(existingSlot, existingSlot));
        assertThatThrownBy(() -> moveSlotThrottlingService.bookSlot(
                1L, "Tower A", "A-202", 12L, moveDay, LocalTime.of(14, 0),
                SlotType.MOVE_IN, "TS09BB2222", "VAN", "Mohan", "8888888888"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already has 2");
    }
}
