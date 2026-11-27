package com.manacommunity.api.handover.controller;

import com.manacommunity.api.handover.entity.*;
import com.manacommunity.api.handover.entity.SnagItem.*;
import com.manacommunity.api.handover.entity.FlatInspection.*;
import com.manacommunity.api.handover.entity.CorpusFundTransaction.*;
import com.manacommunity.api.handover.repository.*;
import com.manacommunity.api.handover.service.CorpusCutoverService;
import com.manacommunity.api.handover.service.SnagResolutionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/builder-handover")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BuilderHandoverController {

    private final SnagResolutionService snagService;
    private final CorpusCutoverService cutoverService;
    private final SnagItemRepository snagRepo;
    private final FlatInspectionRepository inspectionRepo;
    private final CorpusFundTransactionRepository txnRepo;
    private final CutoverSnapshotRepository snapshotRepo;

    // ─── Snag Endpoints ───

    @GetMapping("/snags/dashboard")
    public ResponseEntity<?> getSnagDashboard() {
        return ResponseEntity.ok(snagService.getDashboard());
    }

    @PostMapping("/snags/log")
    public ResponseEntity<?> logSnag(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(snagService.logSnag(
                (String) req.get("flatNumber"), (String) req.get("towerName"),
                ((Number) req.getOrDefault("floor", 0)).intValue(),
                SnagCategory.valueOf((String) req.get("category")),
                SnagSeverity.valueOf((String) req.get("severity")),
                RoomLocation.valueOf((String) req.get("roomLocation")),
                (String) req.get("description"), (String) req.get("photoUrl"),
                (String) req.get("reportedBy"), (String) req.get("phone")));
    }

    @PostMapping("/snags/{id}/assign")
    public ResponseEntity<?> assignSnag(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(snagService.assignToSubcontractor(id,
                (String) req.get("subId"), (String) req.get("subName"),
                (String) req.get("trade"), ((Number) req.getOrDefault("targetDays", 7)).intValue()));
    }

    @PostMapping("/snags/{id}/start")
    public ResponseEntity<?> startWork(@PathVariable Long id) {
        return ResponseEntity.ok(snagService.startWork(id));
    }

    @PostMapping("/snags/{id}/submit-reinspection")
    public ResponseEntity<?> submitForReInspection(@PathVariable Long id, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(snagService.submitForReInspection(id, req.get("photoAfterUrl"), req.get("notes")));
    }

    @PostMapping("/snags/{id}/reinspect")
    public ResponseEntity<?> reInspect(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(snagService.reInspect(id, Boolean.TRUE.equals(req.get("passed")), (String) req.get("inspectorName")));
    }

    @GetMapping("/snags/can-sign-off")
    public ResponseEntity<?> canSignOff(@RequestParam String flat, @RequestParam String tower) {
        return ResponseEntity.ok(Map.of("canSignOff", snagService.canSignOffPossession(flat, tower)));
    }

    @PostMapping("/inspection/conduct")
    public ResponseEntity<?> conductInspection(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(snagService.conductInspection(
                (String) req.get("flatNumber"), (String) req.get("towerName"),
                ((Number) req.getOrDefault("floor", 0)).intValue(),
                InspectionPhase.valueOf((String) req.get("phase")),
                (String) req.get("inspectorName"), (String) req.get("buyerName")));
    }

    // ─── Corpus Fund Endpoints ───

    @GetMapping("/corpus/dashboard")
    public ResponseEntity<?> getCorpusDashboard() {
        return ResponseEntity.ok(cutoverService.getDashboard());
    }

    @PostMapping("/corpus/snapshot")
    public ResponseEntity<?> createSnapshot(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(cutoverService.createSnapshot(
                (String) req.get("builderName"),
                ((Number) req.getOrDefault("totalFlats", 1500)).intValue(),
                ((Number) req.getOrDefault("possessionDone", 1200)).intValue()));
    }

    @PostMapping("/corpus/snapshot/{id}/freeze")
    public ResponseEntity<?> freezeBalance(@PathVariable Long id, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(cutoverService.freezeOpeningBalance(id, req.get("frozenBy")));
    }

    @PostMapping("/corpus/transaction/{id}/dispute")
    public ResponseEntity<?> disputeTxn(@PathVariable Long id, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(cutoverService.disputeTransaction(id, req.get("reason"), req.get("raisedBy")));
    }

    @PostMapping("/corpus/transaction/{id}/resolve")
    public ResponseEntity<?> resolveTxn(@PathVariable Long id, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(cutoverService.resolveDispute(id, req.get("resolution")));
    }

    @PostMapping("/corpus/escrow")
    public ResponseEntity<?> createEscrow(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok(cutoverService.createEscrowHold(
                ((Number) req.get("amount")).doubleValue(),
                (String) req.get("description"), (String) req.get("reason")));
    }

    // ─── Demo Data ───

    @PostConstruct
    public void seedDemoData() {
        if (snagRepo.count() > 0) return;

        // Seed snags across multiple flats
        List<SnagItem> snags = List.of(
            SnagItem.builder().communityId(1L).projectId(1L).flatNumber("A-1501").towerName("Tower A").floorNumber(15)
                .snagCode("SNAG-TA-1501-001").category(SnagCategory.WATERPROOFING).severity(SnagSeverity.CRITICAL)
                .status(SnagStatus.ASSIGNED).roomLocation(RoomLocation.BATHROOM_MASTER)
                .description("Severe seepage from ceiling, water stains spreading to bedroom wall")
                .reportedByName("Ramesh Agarwal").reportedAt(LocalDateTime.now().minusDays(12))
                .assignedSubcontractorId("SUB-WP-01").assignedSubcontractorName("Sharma Waterproofing")
                .subcontractorTrade("Waterproofing").assignedAt(LocalDateTime.now().minusDays(10))
                .targetResolutionDate(LocalDate.now().plusDays(3))
                .possessionBlocker(true).reworkCount(1).reInspectionCount(1).build(),

            SnagItem.builder().communityId(1L).projectId(1L).flatNumber("A-1501").towerName("Tower A").floorNumber(15)
                .snagCode("SNAG-TA-1501-002").category(SnagCategory.ELECTRICAL).severity(SnagSeverity.CRITICAL)
                .status(SnagStatus.WORK_IN_PROGRESS).roomLocation(RoomLocation.KITCHEN)
                .description("Faulty wiring in kitchen — MCB tripping on appliance load")
                .reportedByName("Ramesh Agarwal").reportedAt(LocalDateTime.now().minusDays(10))
                .assignedSubcontractorId("SUB-EL-01").assignedSubcontractorName("Jain Electricals")
                .subcontractorTrade("Electrical").assignedAt(LocalDateTime.now().minusDays(8))
                .targetResolutionDate(LocalDate.now().plusDays(2))
                .possessionBlocker(true).reworkCount(0).reInspectionCount(0).build(),

            SnagItem.builder().communityId(1L).projectId(1L).flatNumber("A-1501").towerName("Tower A").floorNumber(15)
                .snagCode("SNAG-TA-1501-003").category(SnagCategory.FLOORING).severity(SnagSeverity.MINOR)
                .status(SnagStatus.RE_INSPECTION_PASSED).roomLocation(RoomLocation.LIVING_ROOM)
                .description("Cracked vitrified tile near entrance — 2x2 ft patch").photoAfterUrl("/photos/fixed-tile.jpg")
                .reportedByName("Ramesh Agarwal").reportedAt(LocalDateTime.now().minusDays(15))
                .assignedSubcontractorId("SUB-FL-01").assignedSubcontractorName("Gupta Flooring")
                .subcontractorTrade("Flooring").assignedAt(LocalDateTime.now().minusDays(13))
                .resolvedAt(LocalDateTime.now().minusDays(5)).reInspectedByName("QA Inspector Priya")
                .reInspectionDate(LocalDateTime.now().minusDays(4))
                .possessionBlocker(false).reworkCount(0).reInspectionCount(1).build(),

            SnagItem.builder().communityId(1L).projectId(1L).flatNumber("B-802").towerName("Tower B").floorNumber(8)
                .snagCode("SNAG-TB-802-001").category(SnagCategory.PAINTING).severity(SnagSeverity.COSMETIC)
                .status(SnagStatus.LOGGED).roomLocation(RoomLocation.MASTER_BEDROOM)
                .description("Uneven paint finish on accent wall, visible brush strokes")
                .reportedByName("Priya Sharma").reportedAt(LocalDateTime.now().minusDays(3))
                .possessionBlocker(false).reworkCount(0).reInspectionCount(0).build(),

            SnagItem.builder().communityId(1L).projectId(1L).flatNumber("B-802").towerName("Tower B").floorNumber(8)
                .snagCode("SNAG-TB-802-002").category(SnagCategory.PLUMBING).severity(SnagSeverity.MAJOR)
                .status(SnagStatus.RE_INSPECTION_PENDING).roomLocation(RoomLocation.BATHROOM_COMMON)
                .description("Hot water line not connected to geyser outlet")
                .reportedByName("Priya Sharma").reportedAt(LocalDateTime.now().minusDays(8))
                .assignedSubcontractorId("SUB-PL-01").assignedSubcontractorName("Kumar Plumbing")
                .subcontractorTrade("Plumbing").assignedAt(LocalDateTime.now().minusDays(6))
                .resolvedAt(LocalDateTime.now().minusDays(1)).resolutionNotes("Connected hot water line, tested flow")
                .possessionBlocker(true).reworkCount(0).reInspectionCount(0).build(),

            SnagItem.builder().communityId(1L).projectId(1L).flatNumber("C-304").towerName("Tower C").floorNumber(3)
                .snagCode("SNAG-TC-304-001").category(SnagCategory.GLASS_ALUMINUM).severity(SnagSeverity.MAJOR)
                .status(SnagStatus.DISPUTED).roomLocation(RoomLocation.BALCONY)
                .description("Sliding door track misaligned — door doesn't close flush").disputeReason("Builder claims within tolerance, buyer disagrees")
                .reportedByName("Vikram Reddy").reportedAt(LocalDateTime.now().minusDays(20))
                .possessionBlocker(true).reworkCount(0).reInspectionCount(0).build()
        );
        snagRepo.saveAll(snags);

        // Seed inspections
        inspectionRepo.saveAll(List.of(
            FlatInspection.builder().communityId(1L).projectId(1L).flatNumber("A-1501").towerName("Tower A").floorNumber(15)
                .phase(InspectionPhase.PRE_POSSESSION).outcome(InspectionOutcome.FAIL)
                .inspectorName("QA Inspector Priya").inspectorRole("QA Engineer").buyerName("Ramesh Agarwal")
                .scheduledAt(LocalDateTime.now().minusDays(12)).startedAt(LocalDateTime.now().minusDays(12))
                .completedAt(LocalDateTime.now().minusDays(12).plusMinutes(120)).durationMinutes(120)
                .totalSnagsFound(3).criticalSnagsOpen(2).majorSnagsOpen(0).minorSnagsOpen(1)
                .possessionSignOffBlocked(true).build(),
            FlatInspection.builder().communityId(1L).projectId(1L).flatNumber("B-802").towerName("Tower B").floorNumber(8)
                .phase(InspectionPhase.PRE_POSSESSION).outcome(InspectionOutcome.CONDITIONAL_PASS)
                .inspectorName("QA Inspector Rahul").inspectorRole("QA Engineer").buyerName("Priya Sharma")
                .scheduledAt(LocalDateTime.now().minusDays(8)).startedAt(LocalDateTime.now().minusDays(8))
                .completedAt(LocalDateTime.now().minusDays(8).plusMinutes(90)).durationMinutes(90)
                .totalSnagsFound(2).criticalSnagsOpen(0).majorSnagsOpen(1).minorSnagsOpen(1)
                .possessionSignOffBlocked(false).build()
        ));

        // Seed corpus fund transactions (18 months of historical vouchers)
        txnRepo.saveAll(List.of(
            buildTxn("VCH-001", TransactionType.BUILDER_DEPOSIT, 80000000, "Corpus fund deposit by builder", "2025-26", 4, "Sunrise Builders Pvt Ltd", TransactionStatus.VERIFIED),
            buildTxn("VCH-002", TransactionType.MAINTENANCE_EXPENSE, -1200000, "Common area maintenance Q1", "2025-26", 5, "Green Services Pvt Ltd", TransactionStatus.VERIFIED),
            buildTxn("VCH-003", TransactionType.UTILITY_PAYMENT, -850000, "Electricity bill Apr-Jun", "2025-26", 6, "BESCOM", TransactionStatus.VERIFIED),
            buildTxn("VCH-004", TransactionType.STAFF_SALARY, -600000, "Security & housekeeping staff Q1", "2025-26", 6, "Staff Payroll", TransactionStatus.VERIFIED),
            buildTxn("VCH-005", TransactionType.REPAIR_FUND, -450000, "STP pump overhaul", "2025-26", 7, "Grundfos Service Center", TransactionStatus.VERIFIED),
            buildTxn("VCH-006", TransactionType.INSURANCE_PREMIUM, -320000, "Building insurance annual premium", "2025-26", 8, "New India Assurance", TransactionStatus.VERIFIED),
            buildTxn("VCH-007", TransactionType.MAINTENANCE_EXPENSE, -1350000, "Common area maintenance Q2", "2025-26", 9, "Green Services Pvt Ltd", TransactionStatus.VERIFIED),
            buildTxn("VCH-008", TransactionType.UTILITY_PAYMENT, -920000, "Electricity bill Jul-Sep", "2025-26", 9, "BESCOM", TransactionStatus.VERIFIED),
            buildTxn("VCH-009", TransactionType.STAFF_SALARY, -600000, "Security & housekeeping staff Q2", "2025-26", 9, "Staff Payroll", TransactionStatus.VERIFIED),
            buildTxn("VCH-010", TransactionType.LEGAL_EXPENSE, -180000, "RWA registration & legal fees", "2025-26", 10, "Advocate K. Sharma & Associates", TransactionStatus.DISPUTED),
            buildTxn("VCH-011", TransactionType.SINKING_FUND, -500000, "Sinking fund allocation H1", "2025-26", 10, "Sinking Fund Account", TransactionStatus.VERIFIED),
            buildTxn("VCH-012", TransactionType.MAINTENANCE_EXPENSE, -1400000, "Common area maintenance Q3", "2025-26", 12, "Green Services Pvt Ltd", TransactionStatus.PENDING_VERIFICATION),
            buildTxn("VCH-013", TransactionType.INTEREST_EARNED, 420000, "FD interest earned on corpus", "2025-26", 12, "HDFC Bank", TransactionStatus.VERIFIED),
            buildTxn("VCH-014", TransactionType.ESCROW_HOLD, 2500000, "Disputed charges held in escrow", "2026-27", 1, "Escrow Account", TransactionStatus.VERIFIED),
            buildTxn("VCH-015", TransactionType.UTILITY_PAYMENT, -780000, "Electricity bill Oct-Dec", "2026-27", 1, "BESCOM", TransactionStatus.PENDING_VERIFICATION)
        ));

        // Seed cutover snapshot
        cutoverService.createSnapshot("Sunrise Builders Pvt Ltd", 1500, 1200);
    }

    private CorpusFundTransaction buildTxn(String voucher, TransactionType type, double amount,
                                             String desc, String fy, int month, String party, TransactionStatus status) {
        double gst = amount > 0 ? 0 : Math.abs(amount) * 0.18;
        double tds = type == TransactionType.STAFF_SALARY ? Math.abs(amount) * 0.1 : 0;
        return CorpusFundTransaction.builder()
                .communityId(1L).projectId(1L)
                .voucherNumber(voucher).type(type).status(status)
                .cutoverPhase(CutoverPhase.PRE_CUTOVER)
                .amount(Math.abs(amount)).gstAmount(gst).tdsDeducted(tds).netAmount(amount)
                .description(desc).category(type.name().replace("_", " "))
                .transactionDate(LocalDate.of(2025, month, 15)).financialYear(fy).monthNumber(month)
                .paidToOrReceivedFrom(party).paymentMode("NEFT")
                .partOfCutover(true)
                .build();
    }
}
