package com.manacommunity.api.procurement.service;

import com.manacommunity.api.procurement.entity.*;
import com.manacommunity.api.procurement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThreeWayMatchingService {

    private static final BigDecimal MAKER_CHECKER_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal AMOUNT_TOLERANCE_PERCENT = new BigDecimal("2"); // 2% tolerance

    private final ProcPurchaseOrderRepository poRepo;
    private final ProcGoodsReceiptRepository grnRepo;
    private final ProcVendorInvoiceRepository invoiceRepo;
    private final ProcApprovalWorkflowRepository approvalRepo;

    @Transactional
    public MatchResult performThreeWayMatch(Long invoiceId) {
        ProcVendorInvoice invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

        ProcPurchaseOrder po = invoice.getPurchaseOrder();
        List<ProcGoodsReceipt> grns = grnRepo.findByPurchaseOrderId(po.getId());

        if (grns.isEmpty()) {
            invoice.setMatchResult(ProcVendorInvoice.MatchResult.NO_GRN);
            invoice.setMatchDiscrepancy("No Goods Receipt Note found for PO " + po.getPoNumber());
            invoice.setStatus(ProcVendorInvoice.InvoiceStatus.MATCH_FAILED);
            invoiceRepo.save(invoice);
            return new MatchResult(false, "NO_GRN", invoice.getMatchDiscrepancy(), null);
        }

        int totalReceived = grns.stream().mapToInt(ProcGoodsReceipt::getQuantityReceived).sum();
        BigDecimal totalReceivedValue = grns.stream().map(ProcGoodsReceipt::getReceivedValue).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> discrepancies = new ArrayList<>();

        int poQuantity = po.getLineItems().stream().mapToInt(ProcPoLineItem::getQuantity).sum();
        boolean quantityMatch = totalReceived >= poQuantity;
        if (!quantityMatch) {
            discrepancies.add(String.format("Quantity: PO=%d, GRN=%d (short by %d)", poQuantity, totalReceived, poQuantity - totalReceived));
        }

        BigDecimal tolerance = po.getTotalAmount().multiply(AMOUNT_TOLERANCE_PERCENT).divide(new BigDecimal("100"));
        BigDecimal amountDiff = invoice.getInvoiceAmount().subtract(totalReceivedValue).abs();
        boolean amountMatch = amountDiff.compareTo(tolerance) <= 0;
        if (!amountMatch) {
            discrepancies.add(String.format("Amount: Invoice=₹%s, GRN=₹%s (diff=₹%s, tolerance=₹%s)",
                    invoice.getInvoiceAmount(), totalReceivedValue, amountDiff, tolerance));
        }

        if (discrepancies.isEmpty()) {
            invoice.setMatchResult(ProcVendorInvoice.MatchResult.FULL_MATCH);
            invoice.setStatus(ProcVendorInvoice.InvoiceStatus.MATCHED);
            invoiceRepo.save(invoice);

            if (invoice.getInvoiceAmount().compareTo(MAKER_CHECKER_THRESHOLD) > 0) {
                ProcApprovalWorkflow workflow = initiateApproval(invoice);
                return new MatchResult(true, "FULL_MATCH",
                        "PO → GRN → Invoice matched. Amount >" + MAKER_CHECKER_THRESHOLD + ", approval initiated.",
                        workflow.getId());
            }
            return new MatchResult(true, "FULL_MATCH", "PO → GRN → Invoice matched. Auto-approved (below threshold).", null);
        }

        ProcVendorInvoice.MatchResult result;
        if (!quantityMatch && !amountMatch) result = ProcVendorInvoice.MatchResult.BOTH_MISMATCH;
        else if (!quantityMatch) result = ProcVendorInvoice.MatchResult.QUANTITY_MISMATCH;
        else result = ProcVendorInvoice.MatchResult.AMOUNT_MISMATCH;

        invoice.setMatchResult(result);
        invoice.setMatchDiscrepancy(String.join("; ", discrepancies));
        invoice.setStatus(ProcVendorInvoice.InvoiceStatus.MATCH_FAILED);
        invoiceRepo.save(invoice);

        return new MatchResult(false, result.name(), String.join("; ", discrepancies), null);
    }

    @Transactional
    public ProcApprovalWorkflow initiateApproval(ProcVendorInvoice invoice) {
        ProcApprovalWorkflow workflow = new ProcApprovalWorkflow();
        workflow.setCommunityId(invoice.getCommunityId());
        workflow.setInvoiceId(invoice.getId());
        workflow.setPoNumber(invoice.getPurchaseOrder().getPoNumber());
        workflow.setInvoiceNumber(invoice.getInvoiceNumber());
        workflow.setAmount(invoice.getInvoiceAmount());
        workflow.setCurrentLevel(ProcApprovalWorkflow.ApprovalLevel.ESTATE_MANAGER);
        workflow.setStatus(ProcApprovalWorkflow.WorkflowStatus.PENDING);

        invoice.setStatus(ProcVendorInvoice.InvoiceStatus.PENDING_APPROVAL);
        invoiceRepo.save(invoice);

        log.info("Approval workflow initiated for invoice {} (₹{}), starting at ESTATE_MANAGER",
                invoice.getInvoiceNumber(), invoice.getInvoiceAmount());
        return approvalRepo.save(workflow);
    }

    @Transactional
    public ApprovalStepResult processApproval(Long workflowId, String approverId, boolean approved, String remarks) {
        ProcApprovalWorkflow wf = approvalRepo.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        if (wf.getStatus() == ProcApprovalWorkflow.WorkflowStatus.FULLY_APPROVED ||
            wf.getStatus() == ProcApprovalWorkflow.WorkflowStatus.REJECTED) {
            return new ApprovalStepResult(false, "Workflow already " + wf.getStatus(), wf.getStatus().name(), null);
        }

        if (!approved) {
            wf.setStatus(ProcApprovalWorkflow.WorkflowStatus.REJECTED);
            wf.setRejectedBy(approverId);
            wf.setRejectionReason(remarks);
            wf.setRejectedAt(LocalDateTime.now());
            approvalRepo.save(wf);

            ProcVendorInvoice invoice = invoiceRepo.findById(wf.getInvoiceId()).orElse(null);
            if (invoice != null) {
                invoice.setStatus(ProcVendorInvoice.InvoiceStatus.REJECTED);
                invoiceRepo.save(invoice);
            }
            log.info("Invoice {} REJECTED by {} at level {}", wf.getInvoiceNumber(), approverId, wf.getCurrentLevel());
            return new ApprovalStepResult(true, "Rejected by " + approverId, "REJECTED", null);
        }

        LocalDateTime now = LocalDateTime.now();
        switch (wf.getCurrentLevel()) {
            case ESTATE_MANAGER -> {
                wf.setEstateManagerId(approverId);
                wf.setEstateManagerApprovedAt(now);
                wf.setEstateManagerRemarks(remarks);
                wf.setStatus(ProcApprovalWorkflow.WorkflowStatus.ESTATE_MANAGER_APPROVED);
                wf.setCurrentLevel(ProcApprovalWorkflow.ApprovalLevel.TREASURER);
                approvalRepo.save(wf);
                return new ApprovalStepResult(true, "Estate Manager approved. Forwarded to Treasurer.",
                        "ESTATE_MANAGER_APPROVED", ProcApprovalWorkflow.ApprovalLevel.TREASURER.name());
            }
            case TREASURER -> {
                wf.setTreasurerId(approverId);
                wf.setTreasurerApprovedAt(now);
                wf.setTreasurerRemarks(remarks);
                wf.setStatus(ProcApprovalWorkflow.WorkflowStatus.TREASURER_APPROVED);
                wf.setCurrentLevel(ProcApprovalWorkflow.ApprovalLevel.SECRETARY);
                approvalRepo.save(wf);
                return new ApprovalStepResult(true, "Treasurer approved. Forwarded to Secretary.",
                        "TREASURER_APPROVED", ProcApprovalWorkflow.ApprovalLevel.SECRETARY.name());
            }
            case SECRETARY -> {
                wf.setSecretaryId(approverId);
                wf.setSecretaryApprovedAt(now);
                wf.setSecretaryRemarks(remarks);
                wf.setStatus(ProcApprovalWorkflow.WorkflowStatus.FULLY_APPROVED);
                approvalRepo.save(wf);

                ProcVendorInvoice invoice = invoiceRepo.findById(wf.getInvoiceId()).orElse(null);
                if (invoice != null) {
                    invoice.setStatus(ProcVendorInvoice.InvoiceStatus.APPROVED);
                    invoiceRepo.save(invoice);
                }
                log.info("Invoice {} FULLY APPROVED (₹{})", wf.getInvoiceNumber(), wf.getAmount());
                return new ApprovalStepResult(true, "Secretary approved. Invoice fully approved for payment.",
                        "FULLY_APPROVED", null);
            }
            default -> {
                return new ApprovalStepResult(false, "Unknown approval level", wf.getCurrentLevel().name(), null);
            }
        }
    }

    public List<ProcApprovalWorkflow> getPendingApprovals(Long communityId, ProcApprovalWorkflow.ApprovalLevel level) {
        return approvalRepo.findByCommunityIdAndCurrentLevel(communityId, level);
    }

    public Map<String, Object> getMatchSummary(Long communityId) {
        List<ProcVendorInvoice> invoices = invoiceRepo.findByCommunityId(communityId);
        long matched = invoices.stream().filter(i -> i.getMatchResult() == ProcVendorInvoice.MatchResult.FULL_MATCH).count();
        long failed = invoices.stream().filter(i -> i.getStatus() == ProcVendorInvoice.InvoiceStatus.MATCH_FAILED).count();
        long pending = invoices.stream().filter(i -> i.getStatus() == ProcVendorInvoice.InvoiceStatus.PENDING_MATCH).count();
        long approved = invoices.stream().filter(i -> i.getStatus() == ProcVendorInvoice.InvoiceStatus.APPROVED).count();

        return Map.of("totalInvoices", invoices.size(), "matched", matched, "failed", failed,
                "pendingMatch", pending, "approved", approved,
                "pendingApproval", invoices.stream().filter(i -> i.getStatus() == ProcVendorInvoice.InvoiceStatus.PENDING_APPROVAL).count());
    }

    public record MatchResult(boolean matched, String resultType, String details, Long workflowId) {}
    public record ApprovalStepResult(boolean success, String message, String newStatus, String nextLevel) {}
}
