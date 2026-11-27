package com.manacommunity.api.procurement.controller;

import com.manacommunity.api.procurement.entity.*;
import com.manacommunity.api.procurement.repository.*;
import com.manacommunity.api.procurement.service.AmcSchedulerService;
import com.manacommunity.api.procurement.service.ThreeWayMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendor-procurement")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VendorProcurementController {

    private final ThreeWayMatchingService matchingService;
    private final AmcSchedulerService amcService;
    private final ProcPurchaseOrderRepository poRepo;
    private final ProcGoodsReceiptRepository grnRepo;
    private final ProcVendorInvoiceRepository invoiceRepo;
    private final ProcApprovalWorkflowRepository approvalRepo;
    private final ProcAmcContractRepository contractRepo;
    private final ProcMaintenanceScheduleRepository scheduleRepo;

    // === Three-Way Matching ===

    @PostMapping("/match/{invoiceId}")
    public ResponseEntity<ThreeWayMatchingService.MatchResult> performMatch(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(matchingService.performThreeWayMatch(invoiceId));
    }

    @GetMapping("/match/summary/{communityId}")
    public ResponseEntity<Map<String, Object>> getMatchSummary(@PathVariable Long communityId) {
        return ResponseEntity.ok(matchingService.getMatchSummary(communityId));
    }

    @GetMapping("/invoices/{communityId}")
    public ResponseEntity<List<ProcVendorInvoice>> getInvoices(@PathVariable Long communityId) {
        return ResponseEntity.ok(invoiceRepo.findByCommunityId(communityId));
    }

    @GetMapping("/purchase-orders/{communityId}")
    public ResponseEntity<List<ProcPurchaseOrder>> getPurchaseOrders(@PathVariable Long communityId) {
        return ResponseEntity.ok(poRepo.findByCommunityId(communityId));
    }

    // === Maker-Checker Approvals ===

    @PostMapping("/approval/{workflowId}")
    public ResponseEntity<ThreeWayMatchingService.ApprovalStepResult> processApproval(
            @PathVariable Long workflowId, @RequestBody ApprovalRequest req) {
        return ResponseEntity.ok(matchingService.processApproval(workflowId, req.approverId(), req.approved(), req.remarks()));
    }

    @GetMapping("/approvals/pending/{communityId}")
    public ResponseEntity<List<ProcApprovalWorkflow>> getPendingApprovals(
            @PathVariable Long communityId,
            @RequestParam(defaultValue = "ESTATE_MANAGER") ProcApprovalWorkflow.ApprovalLevel level) {
        return ResponseEntity.ok(matchingService.getPendingApprovals(communityId, level));
    }

    @GetMapping("/approvals/{communityId}")
    public ResponseEntity<List<ProcApprovalWorkflow>> getAllApprovals(@PathVariable Long communityId) {
        return ResponseEntity.ok(approvalRepo.findByCommunityId(communityId));
    }

    // === AMC Contracts ===

    @GetMapping("/amc/contracts/{communityId}")
    public ResponseEntity<List<ProcAmcContract>> getContracts(@PathVariable Long communityId) {
        return ResponseEntity.ok(contractRepo.findByCommunityId(communityId));
    }

    @GetMapping("/amc/contracts/{communityId}/category/{category}")
    public ResponseEntity<List<ProcAmcContract>> getContractsByCategory(
            @PathVariable Long communityId, @PathVariable String category) {
        return ResponseEntity.ok(contractRepo.findByCommunityIdAndAssetCategory(communityId, category));
    }

    @GetMapping("/amc/expiry-alerts/{communityId}")
    public ResponseEntity<List<AmcSchedulerService.ExpiryAlert>> getExpiryAlerts(@PathVariable Long communityId) {
        return ResponseEntity.ok(amcService.checkAmcExpiries(communityId));
    }

    @GetMapping("/amc/dashboard/{communityId}")
    public ResponseEntity<Map<String, Object>> getAmcDashboard(@PathVariable Long communityId) {
        return ResponseEntity.ok(amcService.getDashboardSummary(communityId));
    }

    // === Preventive Maintenance Schedules ===

    @PostMapping("/maintenance/generate/{communityId}")
    public ResponseEntity<AmcSchedulerService.GenerateResult> generateSchedules(
            @PathVariable Long communityId,
            @RequestParam(defaultValue = "3") int monthsAhead) {
        return ResponseEntity.ok(amcService.generateSchedules(communityId, monthsAhead));
    }

    @GetMapping("/maintenance/upcoming/{communityId}")
    public ResponseEntity<List<ProcMaintenanceSchedule>> getUpcomingMaintenance(
            @PathVariable Long communityId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(amcService.getUpcoming(communityId, days));
    }

    @GetMapping("/maintenance/overdue/{communityId}")
    public ResponseEntity<List<ProcMaintenanceSchedule>> getOverdue(@PathVariable Long communityId) {
        return ResponseEntity.ok(amcService.getOverdueSchedules(communityId));
    }

    @PostMapping("/maintenance/complete/{scheduleId}")
    public ResponseEntity<ProcMaintenanceSchedule> completeSchedule(
            @PathVariable Long scheduleId, @RequestBody CompleteScheduleRequest req) {
        return ResponseEntity.ok(amcService.completeSchedule(
                scheduleId, req.technicianName(), req.notes(), req.sparePartsUsed(), req.sparePartDetails()));
    }

    // === Demo Data ===

    @PostMapping("/demo/seed/{communityId}")
    public ResponseEntity<Map<String, Object>> seedDemoData(@PathVariable Long communityId) {
        // Create sample PO
        ProcPurchaseOrder po = new ProcPurchaseOrder();
        po.setCommunityId(communityId);
        po.setPoNumber("PO-" + System.currentTimeMillis());
        po.setVendorName("Schindler India Pvt Ltd");
        po.setVendorGstin("29AABCS1429B1ZB");
        po.setCategory("ELEVATOR");
        po.setTotalAmount(new BigDecimal("185000"));
        po.setGstAmount(new BigDecimal("33300"));
        po.setNetPayable(new BigDecimal("218300"));
        po.setRaisedBy("Estate Manager");
        po.setDescription("Annual elevator maintenance - Tower A (4 lifts)");
        po.setExpectedDeliveryDate(LocalDate.now().plusDays(15));
        po.setStatus(ProcPurchaseOrder.PoStatus.APPROVED);

        ProcPoLineItem item1 = new ProcPoLineItem();
        item1.setPurchaseOrder(po);
        item1.setItemDescription("Elevator servicing - quarterly");
        item1.setQuantity(4);
        item1.setUnitPrice(new BigDecimal("35000"));
        item1.setUnit("SERVICE");
        po.getLineItems().add(item1);

        ProcPoLineItem item2 = new ProcPoLineItem();
        item2.setPurchaseOrder(po);
        item2.setItemDescription("Wire rope replacement");
        item2.setQuantity(2);
        item2.setUnitPrice(new BigDecimal("12500"));
        item2.setUnit("SET");
        po.getLineItems().add(item2);

        po = poRepo.save(po);

        // Create GRN
        ProcGoodsReceipt grn = new ProcGoodsReceipt();
        grn.setGrnNumber("GRN-" + System.currentTimeMillis());
        grn.setPurchaseOrder(po);
        grn.setCommunityId(communityId);
        grn.setQuantityReceived(6);
        grn.setReceivedValue(new BigDecimal("185000"));
        grn.setReceivedBy("Security Supervisor");
        grn.setReceivedAt(java.time.LocalDateTime.now());
        grn.setStatus(ProcGoodsReceipt.GrnStatus.INSPECTED_OK);
        grn = grnRepo.save(grn);

        // Create Invoice
        ProcVendorInvoice invoice = new ProcVendorInvoice();
        invoice.setInvoiceNumber("INV-SCH-" + System.currentTimeMillis());
        invoice.setPurchaseOrder(po);
        invoice.setGoodsReceipt(grn);
        invoice.setCommunityId(communityId);
        invoice.setInvoiceAmount(new BigDecimal("185000"));
        invoice.setGstAmount(new BigDecimal("33300"));
        invoice.setTdsDeducted(new BigDecimal("1850"));
        invoice.setNetPayable(new BigDecimal("216450"));
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice = invoiceRepo.save(invoice);

        // Create AMC contracts
        seedAmcContracts(communityId);

        return ResponseEntity.ok(Map.of("poId", po.getId(), "grnId", grn.getId(), "invoiceId", invoice.getId(),
                "message", "Demo data seeded: 1 PO, 1 GRN, 1 Invoice, AMC contracts for elevators & DGs"));
    }

    private void seedAmcContracts(Long communityId) {
        String[][] elevators = {
                {"ELV-TA-01", "Tower A Lift 1"}, {"ELV-TA-02", "Tower A Lift 2"},
                {"ELV-TB-01", "Tower B Lift 1"}, {"ELV-TB-02", "Tower B Lift 2"},
        };
        for (String[] elv : elevators) {
            ProcAmcContract c = new ProcAmcContract();
            c.setCommunityId(communityId);
            c.setContractNumber("AMC-ELV-" + elv[0]);
            c.setVendorName("Schindler India");
            c.setAssetCategory("ELEVATOR");
            c.setAssetIdentifier(elv[0]);
            c.setAnnualValue(new BigDecimal("85000"));
            c.setStartDate(LocalDate.now().minusMonths(6));
            c.setEndDate(LocalDate.now().plusMonths(6));
            c.setServiceIntervalDays(30);
            c.setSparePartThreshold(10);
            c.setContactPerson("Rajesh Kumar");
            c.setContactPhone("9876543210");
            c.setSlaResponseHours("4");
            contractRepo.save(c);
        }

        String[][] dgs = {{"DG-01", "DG Set 1 (500 KVA)"}, {"DG-02", "DG Set 2 (500 KVA)"}};
        for (String[] dg : dgs) {
            ProcAmcContract c = new ProcAmcContract();
            c.setCommunityId(communityId);
            c.setContractNumber("AMC-DG-" + dg[0]);
            c.setVendorName("Cummins Power Gen");
            c.setAssetCategory("DG_SET");
            c.setAssetIdentifier(dg[0]);
            c.setAnnualValue(new BigDecimal("120000"));
            c.setStartDate(LocalDate.now().minusMonths(10));
            c.setEndDate(LocalDate.now().plusDays(25));
            c.setWarrantyExpiry(LocalDate.now().plusDays(15));
            c.setServiceIntervalDays(90);
            c.setSparePartThreshold(5);
            c.setContactPerson("Suresh Verma");
            c.setContactPhone("9123456780");
            c.setSlaResponseHours("2");
            contractRepo.save(c);
        }

        ProcAmcContract stp = new ProcAmcContract();
        stp.setCommunityId(communityId);
        stp.setContractNumber("AMC-STP-01");
        stp.setVendorName("Enviro Water Minerals");
        stp.setAssetCategory("STP");
        stp.setAssetIdentifier("STP-01");
        stp.setAnnualValue(new BigDecimal("240000"));
        stp.setStartDate(LocalDate.now().minusMonths(3));
        stp.setEndDate(LocalDate.now().plusMonths(9));
        stp.setServiceIntervalDays(15);
        stp.setSparePartThreshold(8);
        stp.setContactPerson("Amit Sharma");
        stp.setContactPhone("9988776655");
        stp.setSlaResponseHours("6");
        contractRepo.save(stp);
    }

    // Request DTOs
    public record ApprovalRequest(String approverId, boolean approved, String remarks) {}
    public record CompleteScheduleRequest(String technicianName, String notes, int sparePartsUsed, String sparePartDetails) {}
}
