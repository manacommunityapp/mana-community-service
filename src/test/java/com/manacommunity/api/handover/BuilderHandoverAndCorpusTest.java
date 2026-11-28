package com.manacommunity.api.handover;

import com.manacommunity.api.handover.entity.CorpusFundTransaction;
import com.manacommunity.api.handover.entity.CorpusFundTransaction.TransactionStatus;
import com.manacommunity.api.handover.entity.CorpusFundTransaction.TransactionType;
import com.manacommunity.api.handover.entity.CutoverSnapshot;
import com.manacommunity.api.handover.entity.CutoverSnapshot.CutoverStatus;
import com.manacommunity.api.handover.entity.FlatInspection;
import com.manacommunity.api.handover.entity.FlatInspection.InspectionOutcome;
import com.manacommunity.api.handover.entity.FlatInspection.InspectionPhase;
import com.manacommunity.api.handover.entity.SnagItem;
import com.manacommunity.api.handover.entity.SnagItem.RoomLocation;
import com.manacommunity.api.handover.entity.SnagItem.SnagCategory;
import com.manacommunity.api.handover.entity.SnagItem.SnagSeverity;
import com.manacommunity.api.handover.entity.SnagItem.SnagStatus;
import com.manacommunity.api.handover.repository.CorpusFundTransactionRepository;
import com.manacommunity.api.handover.repository.CutoverSnapshotRepository;
import com.manacommunity.api.handover.repository.FlatInspectionRepository;
import com.manacommunity.api.handover.repository.SnagItemRepository;
import com.manacommunity.api.handover.service.CorpusCutoverService;
import com.manacommunity.api.handover.service.SnagResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuilderHandoverAndCorpusTest {

    @Mock private SnagItemRepository snagRepo;
    @Mock private FlatInspectionRepository inspectionRepo;
    @Mock private CorpusFundTransactionRepository txnRepo;
    @Mock private CutoverSnapshotRepository snapshotRepo;

    private SnagResolutionService snagService;
    private CorpusCutoverService corpusService;

    private final List<SnagItem> snags = new ArrayList<>();
    private final List<CorpusFundTransaction> txns = new ArrayList<>();

    @BeforeEach
    void setUp() {
        snags.clear();
        txns.clear();
        snagService = new SnagResolutionService(snagRepo, inspectionRepo);
        corpusService = new CorpusCutoverService(txnRepo, snapshotRepo);
    }

    @Test
    void tracksSnagAnnotationSubcontractorReworkLoopAndPossessionBlocking() {
        when(snagRepo.save(any(SnagItem.class))).thenAnswer(inv -> {
            SnagItem s = inv.getArgument(0);
            if (!snags.contains(s)) snags.add(s);
            return s;
        });
        when(snagRepo.findByFlatNumberAndTowerName("B-804", "Tower B")).thenReturn(snags);
        when(inspectionRepo.save(any(FlatInspection.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Log critical bathroom seepage snag with annotated photo coordinates
        SnagItem snag = snagService.logSnag(
                "B-804", "Tower B", 8,
                SnagCategory.WATERPROOFING, SnagSeverity.CRITICAL, RoomLocation.BATHROOM_MASTER,
                "Seepage near shower drain", "https://cdn.example.com/snag1.jpg",
                "Amit Buyer", "9876543210");
        snag.setExactCoordinates("x:0.42,y:0.71");
        snag.setAnnotatedPhotoUrl("https://cdn.example.com/snag1-pinned.jpg");

        when(snagRepo.findById(1L)).thenReturn(Optional.of(snag));

        // Possession blocked while critical snag is open
        assertThat(snagService.canSignOffPossession("B-804", "Tower B")).isFalse();
        FlatInspection blockedWalk = snagService.conductInspection(
                "B-804", "Tower B", 8, InspectionPhase.PRE_POSSESSION, "QA Lead", "Amit Buyer");
        assertThat(blockedWalk.isPossessionSignOffBlocked()).isTrue();
        assertThat(blockedWalk.getOutcome()).isEqualTo(InspectionOutcome.FAIL);

        // 2. Route to plumbing subcontractor with 3-day SLA, start & submit fix
        snagService.assignToSubcontractor(1L, "SUB-PLM-09", "Metro Plumbing", "PLUMBING", 3);
        assertThat(snag.getStatus()).isEqualTo(SnagStatus.ASSIGNED);
        assertThat(snag.getTargetResolutionDate()).isNotNull();

        snagService.startWork(1L);
        snagService.submitForReInspection(1L, "https://cdn.example.com/snag1-after.jpg", "Re-grouted tiles");

        // 3. First re-inspection fails -> triggers REWORK_NEEDED and increments reworkCount
        snagService.reInspect(1L, false, "QA Lead");
        assertThat(snag.getStatus()).isEqualTo(SnagStatus.REWORK_NEEDED);
        assertThat(snag.getReworkCount()).isEqualTo(1);
        assertThat(snagService.canSignOffPossession("B-804", "Tower B")).isFalse();

        // 4. Second re-inspection passes -> clears blocker and unblocks possession
        snagService.reInspect(1L, true, "QA Lead");
        assertThat(snag.getStatus()).isEqualTo(SnagStatus.RE_INSPECTION_PASSED);
        assertThat(snag.isPossessionBlocker()).isFalse();
        assertThat(snagService.canSignOffPossession("B-804", "Tower B")).isTrue();
    }

    @Test
    void computesCorpusOpeningBalanceGstCreditCarryoverEscrowHoldAndFreezesCutover() {
        CorpusFundTransaction corpusDeposit = CorpusFundTransaction.builder()
                .communityId(1L).projectId(1L).voucherNumber("V-001")
                .type(TransactionType.BUILDER_DEPOSIT).status(TransactionStatus.VERIFIED)
                .amount(10_000_000.0).gstAmount(0).netAmount(10_000_000.0).build();

        CorpusFundTransaction verifiedVendorBill = CorpusFundTransaction.builder()
                .communityId(1L).projectId(1L).voucherNumber("V-002")
                .type(TransactionType.MAINTENANCE_EXPENSE).status(TransactionStatus.VERIFIED)
                .amount(1_000_000.0).gstAmount(180_000.0).netAmount(1_180_000.0).build();

        CorpusFundTransaction disputedBill = CorpusFundTransaction.builder()
                .communityId(1L).projectId(1L).voucherNumber("V-003")
                .type(TransactionType.DISPUTED_CHARGE).status(TransactionStatus.PENDING_VERIFICATION)
                .amount(250_000.0).gstAmount(45_000.0).netAmount(250_000.0).build();

        txns.addAll(List.of(corpusDeposit, verifiedVendorBill, disputedBill));

        when(txnRepo.findAll()).thenReturn(txns);
        when(txnRepo.findById(3L)).thenReturn(Optional.of(disputedBill));
        when(txnRepo.save(any(CorpusFundTransaction.class))).thenAnswer(inv -> {
            CorpusFundTransaction t = inv.getArgument(0);
            if (!txns.contains(t)) txns.add(t);
            return t;
        });

        CutoverSnapshot[] savedSnapHolder = new CutoverSnapshot[1];
        when(snapshotRepo.save(any(CutoverSnapshot.class))).thenAnswer(inv -> {
            CutoverSnapshot s = inv.getArgument(0);
            savedSnapHolder[0] = s;
            return s;
        });
        when(snapshotRepo.findById(10L)).thenAnswer(inv -> Optional.ofNullable(savedSnapHolder[0]));

        // Flag disputed vendor bill and place equivalent escrow hold
        corpusService.disputeTransaction(3L, "Unapproved lift AMC bill", "RWA Treasurer");
        assertThat(disputedBill.getStatus()).isEqualTo(TransactionStatus.DISPUTED);

        corpusService.createEscrowHold(250_000.0, "Lift AMC Escrow", "Pending builder arbitration");

        CutoverSnapshot snap = corpusService.createSnapshot("Apex Builders Ltd", 500, 480);

        // Verified GST input credit carried over (180,000), excluding unverified/disputed bill GST
        assertThat(snap.getTotalCorpusFund()).isEqualTo(10_000_000.0);
        assertThat(snap.getTotalEscrowHeld()).isEqualTo(250_000.0);
        assertThat(snap.getDisputedVouchers()).isEqualTo(1);
        assertThat(snap.getGstInputCreditCarryover()).isEqualTo(180_000.0);
        assertThat(snap.getOpeningBalanceRwa()).isEqualTo(10_000_000.0 - (1_180_000.0 + 250_000.0));

        // Freeze opening balance locks all ledger transactions
        CutoverSnapshot frozen = corpusService.freezeOpeningBalance(10L, "RWA Secretary");
        assertThat(frozen.isOpeningBalanceFrozen()).isTrue();
        assertThat(frozen.getStatus()).isEqualTo(CutoverStatus.OPENING_BALANCE_FROZEN);
        assertThat(txns).allSatisfy(t -> assertThat(t.isFrozen()).isTrue());
    }
}
