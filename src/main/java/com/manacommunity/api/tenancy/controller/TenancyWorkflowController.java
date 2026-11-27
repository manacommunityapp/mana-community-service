package com.manacommunity.api.tenancy.controller;

import com.manacommunity.api.tenancy.entity.*;
import com.manacommunity.api.tenancy.entity.TenantDocument.DocumentType;
import com.manacommunity.api.tenancy.entity.MoveSlot.SlotType;
import com.manacommunity.api.tenancy.repository.*;
import com.manacommunity.api.tenancy.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/tenancy")
@RequiredArgsConstructor
public class TenancyWorkflowController {

    private final MoveOutSagaService sagaService;
    private final DocumentVerificationService documentService;
    private final MoveSlotThrottlingService slotService;
    private final TenantRecordRepository tenantRepository;
    private final MoveSlotRepository slotRepository;

    // === Tenant Records ===

    @GetMapping("/tenants")
    public ResponseEntity<List<TenantRecord>> getAllTenants() {
        return ResponseEntity.ok(tenantRepository.findAll());
    }

    @GetMapping("/tenants/{id}")
    public ResponseEntity<TenantRecord> getTenant(@PathVariable Long id) {
        return ResponseEntity.ok(tenantRepository.findById(id).orElseThrow());
    }

    // === Move-Out Saga ===

    @PostMapping("/move-out/initiate/{tenantId}")
    public ResponseEntity<MoveOutSaga> initiateMoveOut(@PathVariable Long tenantId) {
        return ResponseEntity.ok(sagaService.initiateMoveOut(tenantId));
    }

    @PostMapping("/move-out/execute/{sagaId}")
    public ResponseEntity<MoveOutSaga> executeMoveOut(@PathVariable Long sagaId) {
        return ResponseEntity.ok(sagaService.executeSaga(sagaId));
    }

    @PostMapping("/move-out/compensate/{sagaId}")
    public ResponseEntity<MoveOutSaga> compensateMoveOut(@PathVariable Long sagaId) {
        return ResponseEntity.ok(sagaService.compensate(sagaId));
    }

    @GetMapping("/move-out/summary")
    public ResponseEntity<MoveOutSagaService.SagaSummary> getSagaSummary(
            @RequestParam(defaultValue = "1") Long communityId) {
        return ResponseEntity.ok(sagaService.getSagaSummary(communityId));
    }

    // === Document Verification ===

    @PostMapping("/documents/upload")
    public ResponseEntity<TenantDocument> uploadDocument(@RequestBody DocumentUploadRequest req) {
        return ResponseEntity.ok(documentService.uploadDocument(req.tenantRecordId(), req.documentType(), req.fileName()));
    }

    @PostMapping("/documents/review/{documentId}")
    public ResponseEntity<TenantDocument> reviewDocument(@PathVariable Long documentId,
                                                          @RequestBody DocumentReviewRequest req) {
        return ResponseEntity.ok(documentService.reviewDocument(documentId, req.approved(), req.remarks(), req.reviewerName()));
    }

    @GetMapping("/documents/pipeline")
    public ResponseEntity<DocumentVerificationService.DocumentPipelineSummary> getDocumentPipeline() {
        return ResponseEntity.ok(documentService.getPipelineSummary());
    }

    @GetMapping("/documents/expiry-alerts")
    public ResponseEntity<List<DocumentVerificationService.ExpiryAlert>> getExpiryAlerts() {
        return ResponseEntity.ok(documentService.checkExpiryAlerts());
    }

    // === Slot Throttling ===

    @GetMapping("/slots/availability")
    public ResponseEntity<MoveSlotThrottlingService.SlotAvailability> checkSlotAvailability(
            @RequestParam String tower, @RequestParam String date, @RequestParam SlotType type) {
        return ResponseEntity.ok(slotService.checkAvailability(tower, LocalDate.parse(date), type));
    }

    @PostMapping("/slots/book")
    public ResponseEntity<MoveSlot> bookSlot(@RequestBody SlotBookingRequest req) {
        return ResponseEntity.ok(slotService.bookSlot(req.communityId(), req.tower(), req.flatNumber(),
                req.tenantRecordId(), LocalDate.parse(req.date()), LocalTime.parse(req.startTime()),
                req.type(), req.vehicleNumber(), req.vehicleType(), req.driverName(), req.driverPhone()));
    }

    @PostMapping("/slots/gate-pass/{slotId}")
    public ResponseEntity<MoveSlot> issueGatePass(@PathVariable Long slotId) {
        return ResponseEntity.ok(slotService.issueGatePass(slotId));
    }

    @PostMapping("/slots/validate-pass")
    public ResponseEntity<MoveSlot> validateGatePass(@RequestParam String gatePass) {
        return ResponseEntity.ok(slotService.validateGatePass(gatePass));
    }

    @GetMapping("/slots/schedule")
    public ResponseEntity<MoveSlotThrottlingService.SlotScheduleSummary> getDaySchedule(
            @RequestParam(defaultValue = "1") Long communityId,
            @RequestParam(required = false) String date) {
        LocalDate d = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(slotService.getDaySchedule(communityId, d));
    }

    // === Demo Data Seed ===

    @PostMapping("/seed")
    public ResponseEntity<String> seedDemoData() {
        // Active tenants
        TenantRecord t1 = tenantRepository.save(TenantRecord.builder()
                .communityId(1L).flatNumber("A-302").towerName("Tower A")
                .tenantName("Rahul Sharma").tenantPhone("9876543210").tenantEmail("rahul@email.com")
                .ownerId(100L).status(TenantRecord.TenantStatus.ACTIVE)
                .moveInDate(LocalDate.now().minusMonths(8)).leaseStartDate(LocalDate.now().minusMonths(8))
                .leaseEndDate(LocalDate.now().plusMonths(3))
                .appAccessActive(true).activeRfidTags(3).linkedDailyHelps(2).pendingDues(0)
                .kycStatus(TenantRecord.KycStatus.APPROVED).build());

        TenantRecord t2 = tenantRepository.save(TenantRecord.builder()
                .communityId(1L).flatNumber("B-105").towerName("Tower B")
                .tenantName("Priya Patel").tenantPhone("9876543211").tenantEmail("priya@email.com")
                .ownerId(101L).status(TenantRecord.TenantStatus.ACTIVE)
                .moveInDate(LocalDate.now().minusMonths(5)).leaseStartDate(LocalDate.now().minusMonths(5))
                .leaseEndDate(LocalDate.now().plusMonths(6))
                .appAccessActive(true).activeRfidTags(2).linkedDailyHelps(3).pendingDues(4500)
                .kycStatus(TenantRecord.KycStatus.APPROVED).build());

        TenantRecord t3 = tenantRepository.save(TenantRecord.builder()
                .communityId(1L).flatNumber("C-801").towerName("Tower C")
                .tenantName("Amit Kumar").tenantPhone("9876543212").tenantEmail("amit@email.com")
                .ownerId(102L).status(TenantRecord.TenantStatus.KYC_PENDING)
                .leaseStartDate(LocalDate.now()).leaseEndDate(LocalDate.now().plusMonths(11))
                .appAccessActive(false).activeRfidTags(0).linkedDailyHelps(0).pendingDues(0)
                .kycStatus(TenantRecord.KycStatus.NOT_STARTED).build());

        TenantRecord t4 = tenantRepository.save(TenantRecord.builder()
                .communityId(1L).flatNumber("A-507").towerName("Tower A")
                .tenantName("Sneha Reddy").tenantPhone("9876543213").tenantEmail("sneha@email.com")
                .ownerId(103L).status(TenantRecord.TenantStatus.ACTIVE)
                .moveInDate(LocalDate.now().minusMonths(10)).leaseStartDate(LocalDate.now().minusMonths(10))
                .leaseEndDate(LocalDate.now().plusMonths(1))
                .appAccessActive(true).activeRfidTags(1).linkedDailyHelps(1).pendingDues(0)
                .kycStatus(TenantRecord.KycStatus.APPROVED).build());

        // Upload documents for KYC-pending tenant
        documentService.uploadDocument(t3.getId(), DocumentType.AADHAAR, "amit_aadhaar.pdf");
        documentService.uploadDocument(t3.getId(), DocumentType.RENTAL_AGREEMENT, "amit_rental_agreement.pdf");
        documentService.uploadDocument(t3.getId(), DocumentType.POLICE_VERIFICATION, "amit_police_verification.pdf");

        // Some move slots
        slotRepository.save(MoveSlot.builder()
                .communityId(1L).towerName("Tower A").flatNumber("D-201").tenantRecordId(null)
                .slotDate(LocalDate.now()).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(12, 0))
                .slotType(SlotType.MOVE_IN).serviceElevatorId("Tower A-SVC-1")
                .status(MoveSlot.SlotStatus.GATE_PASS_ISSUED).gatePassIssued(true)
                .gatePassNumber("GP-MI-001-5678").vehicleNumber("TS09AB1234")
                .vehicleType("Tempo").driverName("Raju").driverPhone("9999988888").build());

        slotRepository.save(MoveSlot.builder()
                .communityId(1L).towerName("Tower B").flatNumber("B-402").tenantRecordId(null)
                .slotDate(LocalDate.now()).startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(16, 0))
                .slotType(SlotType.MOVE_OUT).serviceElevatorId("Tower B-SVC-2")
                .status(MoveSlot.SlotStatus.APPROVED).gatePassIssued(false)
                .vehicleNumber("AP07CD5678").vehicleType("Mini Truck")
                .driverName("Suresh").driverPhone("9999977777").build());

        return ResponseEntity.ok("Seeded 4 tenants, 3 documents, 2 move slots");
    }

    // Request DTOs
    public record DocumentUploadRequest(Long tenantRecordId, DocumentType documentType, String fileName) {}
    public record DocumentReviewRequest(boolean approved, String remarks, String reviewerName) {}
    public record SlotBookingRequest(Long communityId, String tower, String flatNumber, Long tenantRecordId,
                                      String date, String startTime, SlotType type,
                                      String vehicleNumber, String vehicleType, String driverName, String driverPhone) {}
}
