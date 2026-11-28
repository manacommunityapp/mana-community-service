package com.manacommunity.api.service;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Expense;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.repository.ExpenseRepository;
import com.manacommunity.api.repository.InvoiceRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private static final BigDecimal GST_RATE = new BigDecimal("0.09");
    private static final int DUE_DAYS = 30;

    private final ExpenseRepository expenseRepository;
    private final InvoiceRepository invoiceRepository;
    private final AppUserRepository appUserRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manacommunity.platform.sqs.SqsMessagePublisher sqsMessagePublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.manacommunity.platform.sqs.SqsProperties sqsProperties;

    public BillingService(ExpenseRepository expenseRepository,
                          InvoiceRepository invoiceRepository,
                          AppUserRepository appUserRepository) {
        this.expenseRepository = expenseRepository;
        this.invoiceRepository = invoiceRepository;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Calculates the GST breakdown for a given taxable amount.
     * Indian GST: 9% CGST + 9% SGST = 18% total.
     */
    public GstBreakdown calculateGst(BigDecimal taxableAmount) {
        BigDecimal cgst = taxableAmount.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = taxableAmount.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxableAmount.add(cgst).add(sgst);
        return new GstBreakdown(taxableAmount, cgst, sgst, total);
    }

    /**
     * Enqueues batch invoice + mock PDF generation for all active residents onto
     * {@code mana-report-jobs} after the approving transaction commits, falling back
     * to direct in-process execution when SQS is disabled.
     *
     * @param expenseId the approved expense to bill for
     * @return CompletableFuture with the generated count (0 immediately when offloaded to SQS)
     */
    @Transactional
    public CompletableFuture<Integer> batchGenerateInvoices(Long expenseId) {
        if (sqsMessagePublisher != null && sqsProperties != null) {
            var payload = new com.manacommunity.api.events.sqs.ReportJobSqsHandler.BatchInvoicePayload(expenseId);
            java.util.concurrent.atomic.AtomicInteger fallbackCount = new java.util.concurrent.atomic.AtomicInteger(0);
            sqsMessagePublisher.publishAfterCommit(
                    sqsProperties.getQueues().getReportJobs(),
                    "BATCH_INVOICE_GENERATION",
                    payload,
                    () -> fallbackCount.set(executeBatchGenerateInvoices(expenseId))
            );
            return CompletableFuture.completedFuture(fallbackCount.get());
        }
        return CompletableFuture.completedFuture(executeBatchGenerateInvoices(expenseId));
    }

    /**
     * Executes batch invoice generation in a dedicated transaction. Invoked by
     * {@link com.manacommunity.api.events.sqs.ReportJobSqsHandler} on SQS consumption
     * or directly as the local fallback.
     */
    @Transactional
    public int executeBatchGenerateInvoices(Long expenseId) {
        log.info("Starting batch invoice generation for expense {}", expenseId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + expenseId));

        Community community = expense.getCommunity();

        List<AppUser> residents = appUserRepository.findByCommunityIdAndIsActiveTrue(community.getId());
        if (residents.isEmpty()) {
            log.warn("No active residents found for community {} — skipping invoice generation", community.getId());
            return 0;
        }

        BigDecimal perResidentAmount = expense.getAmount()
                .divide(BigDecimal.valueOf(residents.size()), 2, RoundingMode.CEILING);

        AtomicInteger sequence = new AtomicInteger(1);
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        LocalDate dueDate = LocalDate.now().plusDays(DUE_DAYS);

        int generated = 0;
        for (AppUser resident : residents) {
            GstBreakdown gst = calculateGst(perResidentAmount);

            String invoiceNumber = String.format("INV-%s-%04d", datePrefix, sequence.getAndIncrement());

            Invoice invoice = Invoice.builder()
                    .invoiceNumber(invoiceNumber)
                    .resident(resident)
                    .community(community)
                    .expense(expense)
                    .taxableAmount(gst.taxableAmount())
                    .cgst(gst.cgst())
                    .sgst(gst.sgst())
                    .totalAmount(gst.total())
                    .dueDate(dueDate)
                    .status("UNPAID")
                    .build();

            invoiceRepository.save(invoice);
            generated++;

            // Mock S3 PDF upload — replace with real AWS SDK call in production
            String pdfKey = uploadInvoicePdf(invoice);
            invoice.setPdfUrl(pdfKey);
            invoiceRepository.save(invoice);
        }

        log.info("Batch invoice generation complete: {} invoices for expense {} in community {}",
                generated, expenseId, community.getId());

        return generated;
    }

    /**
     * Marks an invoice as paid.
     */
    @Transactional
    public Invoice markAsPaid(Long invoiceId, Long communityId) {
        Invoice invoice = invoiceRepository.findByIdAndCommunityId(invoiceId, communityId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        invoice.setStatus("PAID");
        invoice.setPaidAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    /**
     * Mock S3 PDF upload — generates a mock S3 key for the invoice PDF.
     * In production, this would:
     *  1. Render the invoice to PDF (using iText, OpenPDF, or Thymeleaf + Flying Saucer)
     *  2. Upload to S3 via AWS SDK
     *  3. Return the S3 object URL
     */
    private String uploadInvoicePdf(Invoice invoice) {
        String s3Key = String.format("invoices/%s/%s/%s.pdf",
                invoice.getCommunity().getId(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")),
                invoice.getInvoiceNumber());

        log.info("Mock S3 upload: s3://mana-community-invoices/{}", s3Key);

        // Production implementation:
        // PutObjectRequest putReq = PutObjectRequest.builder()
        //     .bucket("mana-community-invoices")
        //     .key(s3Key)
        //     .contentType("application/pdf")
        //     .build();
        // s3Client.putObject(putReq, RequestBody.fromBytes(pdfBytes));

        return "https://mana-community-invoices.s3.ap-south-1.amazonaws.com/" + s3Key;
    }

    public record GstBreakdown(
            BigDecimal taxableAmount,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal total
    ) {}
}
