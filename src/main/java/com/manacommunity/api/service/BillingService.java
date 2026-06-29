package com.manacommunity.api.service;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Expense;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.repository.ExpenseRepository;
import com.manacommunity.api.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
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
     * Asynchronously generates invoices for all active residents in the
     * community when an expense is approved. The expense total is split
     * equally across all residents, with GST applied per invoice.
     *
     * @param expenseId the approved expense to bill for
     * @return CompletableFuture with the count of generated invoices
     */
    @Async
    @Transactional
    public CompletableFuture<Integer> batchGenerateInvoices(Long expenseId) {
        log.info("Starting batch invoice generation for expense {}", expenseId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + expenseId));

        Community community = expense.getCommunity();

        List<AppUser> residents = appUserRepository.findByCommunityIdAndIsActiveTrue(community.getId());
        if (residents.isEmpty()) {
            log.warn("No active residents found for community {} — skipping invoice generation", community.getId());
            return CompletableFuture.completedFuture(0);
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

        return CompletableFuture.completedFuture(generated);
    }

    /**
     * Marks an invoice as paid.
     */
    @Transactional
    public Invoice markAsPaid(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
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
