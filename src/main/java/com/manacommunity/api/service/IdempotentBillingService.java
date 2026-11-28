package com.manacommunity.api.service;

import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class IdempotentBillingService {

    private static final Logger log = LoggerFactory.getLogger(IdempotentBillingService.class);
    private static final BigDecimal GST_RATE = new BigDecimal("0.09");
    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.02");

    private final BillingRunRepository billingRunRepo;
    private final BillingRunLineItemRepository lineItemRepo;
    private final InvoiceRepository invoiceRepo;
    private final AppUserRepository appUserRepo;
    private final CommunityRepository communityRepo;
    private final LedgerService ledgerService;

    public IdempotentBillingService(BillingRunRepository billingRunRepo,
                                    BillingRunLineItemRepository lineItemRepo,
                                    InvoiceRepository invoiceRepo,
                                    AppUserRepository appUserRepo,
                                    CommunityRepository communityRepo,
                                    LedgerService ledgerService) {
        this.billingRunRepo = billingRunRepo;
        this.lineItemRepo = lineItemRepo;
        this.invoiceRepo = invoiceRepo;
        this.appUserRepo = appUserRepo;
        this.communityRepo = communityRepo;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public BillingRun startOrResumeBillingRun(Long communityId, int month, int year,
                                               BigDecimal maintenancePerSqft,
                                               BigDecimal sinkingFund,
                                               BigDecimal waterCharges,
                                               BigDecimal parkingCharges,
                                               boolean applyGst, boolean applyPenalty,
                                               int dueDays) {
        String idempotencyKey = String.format("MAINT-%d-%04d%02d", communityId, year, month);

        Optional<BillingRun> existing = billingRunRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            BillingRun run = existing.get();
            if ("COMPLETED".equals(run.getStatus())) {
                log.info("Billing run {} already completed — returning existing", idempotencyKey);
                return run;
            }
            if ("RUNNING".equals(run.getStatus())) {
                log.info("Billing run {} in progress — resuming from flat {}", idempotencyKey, run.getLastProcessedFlatId());
                return resumeBillingRun(run, maintenancePerSqft, sinkingFund, waterCharges,
                                         parkingCharges, applyGst, applyPenalty, dueDays);
            }
        }

        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new IllegalArgumentException("Community not found: " + communityId));

        List<AppUser> residents = appUserRepo.findByCommunityIdAndIsActiveTrue(communityId);
        if (residents.isEmpty()) {
            log.warn("No active residents for community {} — aborting billing run", communityId);
            throw new IllegalStateException("No active residents found");
        }

        BillingRun run = BillingRun.builder()
                .idempotencyKey(idempotencyKey)
                .community(community)
                .billingMonth(month)
                .billingYear(year)
                .runType("MAINTENANCE")
                .status("RUNNING")
                .totalFlats(residents.size())
                .processedFlats(0)
                .failedFlats(0)
                .totalAmount(BigDecimal.ZERO)
                .gstAmount(BigDecimal.ZERO)
                .penaltyAmount(BigDecimal.ZERO)
                .startedAt(LocalDateTime.now())
                .build();
        run = billingRunRepo.save(run);

        return executeBillingRun(run, residents, maintenancePerSqft, sinkingFund,
                                  waterCharges, parkingCharges, applyGst, applyPenalty, dueDays);
    }

    private BillingRun resumeBillingRun(BillingRun run, BigDecimal maintenancePerSqft,
                                         BigDecimal sinkingFund, BigDecimal waterCharges,
                                         BigDecimal parkingCharges, boolean applyGst,
                                         boolean applyPenalty, int dueDays) {
        Long lastProcessed = run.getLastProcessedFlatId();
        List<AppUser> remaining = appUserRepo.findByCommunityIdAndIsActiveTrue(run.getCommunity().getId())
                .stream()
                .filter(r -> lastProcessed == null || r.getId() > lastProcessed)
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();

        if (remaining.isEmpty()) {
            run.setStatus("COMPLETED");
            run.setCompletedAt(LocalDateTime.now());
            return billingRunRepo.save(run);
        }

        return executeBillingRun(run, remaining, maintenancePerSqft, sinkingFund,
                                  waterCharges, parkingCharges, applyGst, applyPenalty, dueDays);
    }

    private BillingRun executeBillingRun(BillingRun run, List<AppUser> residents,
                                          BigDecimal maintenancePerSqft, BigDecimal sinkingFund,
                                          BigDecimal waterCharges, BigDecimal parkingCharges,
                                          boolean applyGst, boolean applyPenalty, int dueDays) {
        LocalDate dueDate = LocalDate.of(run.getBillingYear(), run.getBillingMonth(), 1).plusDays(dueDays);
        String datePrefix = String.format("%04d%02d", run.getBillingYear(), run.getBillingMonth());
        long baseSeq = invoiceRepo.countByCommunityId(run.getCommunity().getId());
        BigDecimal runTotal = run.getTotalAmount() != null ? run.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal runGst = run.getGstAmount() != null ? run.getGstAmount() : BigDecimal.ZERO;
        BigDecimal runPenalty = run.getPenaltyAmount() != null ? run.getPenaltyAmount() : BigDecimal.ZERO;

        StringBuilder errorLog = new StringBuilder();

        for (AppUser resident : residents) {
            Optional<BillingRunLineItem> existingLine =
                    lineItemRepo.findByBillingRunIdAndResidentId(run.getId(), resident.getId());
            if (existingLine.isPresent() && "GENERATED".equals(existingLine.get().getStatus())) {
                continue;
            }

            try {
                BigDecimal maintenance = maintenancePerSqft.multiply(BigDecimal.TEN);
                BigDecimal taxable = maintenance.add(sinkingFund).add(waterCharges).add(parkingCharges);

                BigDecimal penalty = BigDecimal.ZERO;
                if (applyPenalty) {
                    long overdueCount = invoiceRepo.countByResidentIdAndStatus(resident.getId(), "OVERDUE");
                    if (overdueCount > 0) {
                        penalty = taxable.multiply(PENALTY_RATE).setScale(2, RoundingMode.HALF_UP);
                    }
                }

                BigDecimal cgst = BigDecimal.ZERO;
                BigDecimal sgst = BigDecimal.ZERO;
                if (applyGst) {
                    cgst = taxable.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
                    sgst = taxable.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
                }

                BigDecimal total = taxable.add(cgst).add(sgst).add(penalty);
                String invoiceNumber = String.format("INV-%s-%04d", datePrefix, baseSeq + run.getProcessedFlats() + 1);

                Invoice invoice = Invoice.builder()
                        .invoiceNumber(invoiceNumber)
                        .resident(resident)
                        .community(run.getCommunity())
                        .taxableAmount(taxable)
                        .cgst(cgst)
                        .sgst(sgst)
                        .totalAmount(total)
                        .dueDate(dueDate)
                        .status("UNPAID")
                        .build();
                invoice = invoiceRepo.save(invoice);

                BillingRunLineItem lineItem = BillingRunLineItem.builder()
                        .billingRun(run)
                        .resident(resident)
                        .flatNo(resident.getFlatNo())
                        .maintenanceAmount(maintenance)
                        .sinkingFund(sinkingFund)
                        .waterCharges(waterCharges)
                        .parkingCharges(parkingCharges)
                        .penaltyAmount(penalty)
                        .cgst(cgst)
                        .sgst(sgst)
                        .totalAmount(total)
                        .dueDate(dueDate)
                        .status("GENERATED")
                        .invoice(invoice)
                        .processedAt(LocalDateTime.now())
                        .build();
                lineItemRepo.save(lineItem);

                ledgerService.recordBillingEntry(run.getCommunity().getId(), invoice);

                run.setProcessedFlats(run.getProcessedFlats() + 1);
                run.setLastProcessedFlatId(resident.getId());
                runTotal = runTotal.add(total);
                runGst = runGst.add(cgst).add(sgst);
                runPenalty = runPenalty.add(penalty);

            } catch (Exception e) {
                log.error("Failed to process billing for resident {} in run {}",
                          resident.getId(), run.getId(), e);
                run.setFailedFlats(run.getFailedFlats() + 1);
                errorLog.append(String.format("Flat %s (user %d): %s\n",
                        resident.getFlatNo(), resident.getId(), e.getMessage()));

                BillingRunLineItem failedItem = BillingRunLineItem.builder()
                        .billingRun(run)
                        .resident(resident)
                        .flatNo(resident.getFlatNo())
                        .maintenanceAmount(BigDecimal.ZERO)
                        .totalAmount(BigDecimal.ZERO)
                        .dueDate(dueDate)
                        .status("FAILED")
                        .errorMessage(e.getMessage())
                        .processedAt(LocalDateTime.now())
                        .build();
                lineItemRepo.save(failedItem);
            }
        }

        run.setTotalAmount(runTotal);
        run.setGstAmount(runGst);
        run.setPenaltyAmount(runPenalty);
        run.setStatus("COMPLETED");
        run.setCompletedAt(LocalDateTime.now());
        if (!errorLog.isEmpty()) {
            run.setErrorLog(errorLog.toString());
        }
        return billingRunRepo.save(run);
    }

    @Transactional(readOnly = true)
    public List<BillingRun> getBillingRuns(Long communityId) {
        return billingRunRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
    }

    @Transactional(readOnly = true)
    public BillingRun getBillingRunDetail(Long runId) {
        return billingRunRepo.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Billing run not found: " + runId));
    }

    @Transactional(readOnly = true)
    public List<BillingRunLineItem> getLineItems(Long runId) {
        return lineItemRepo.findByBillingRunId(runId);
    }
}
