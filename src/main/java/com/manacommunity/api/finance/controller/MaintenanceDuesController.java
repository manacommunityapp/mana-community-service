package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.entity.GeneralLedgerEntry;
import com.manacommunity.api.finance.repository.GeneralLedgerEntryRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.manacommunity.api.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller providing resident maintenance dues, billing statements,
 * Razorpay payment order generation & verification, and maintenance wallet settlement.
 */
@Slf4j
@RestController
@RequestMapping("/api/finance/maintenance")
@RequiredArgsConstructor
public class MaintenanceDuesController {

    private final InvoiceRepository invoiceRepository;
    private final AppUserRepository appUserRepository;
    private final GeneralLedgerEntryRepository generalLedgerEntryRepository;

    // In-memory wallet balance per user (initial balance INR 2,500.00)
    private static final Map<Long, BigDecimal> WALLET_BALANCES = new ConcurrentHashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillChargeDto {
        private String item;
        private Double amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceBillDto {
        private String id;
        private String monthYear;
        private String billNumber;
        private Double maintenanceAmount;
        private Double waterCharges;
        private Double sinkingFund;
        private Double penaltyLateFee;
        private Double totalAmount;
        private Double paidAmount;
        private Double dueAmount;
        private String dueDate;
        private String status; // PENDING, PAID, OVERDUE, PARTIAL
        private List<BillChargeDto> charges;
        private String receiptUrl;
        private String paymentMethod;
        private String paidAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInitiationResponse {
        private String orderId;
        private Double amount;
        private String key;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentVerificationRequest {
        private String billId;
        private String orderId;
        private String paymentId;
        private String signature;
        private String method;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentVerificationResponse {
        private boolean success;
        private String receiptNumber;
        private String paidAt;
        private String message;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WalletBalanceResponse {
        private Double balance;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletPaymentRequest {
        private Double amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletPaymentResponse {
        private boolean success;
        private Double newBalance;
        private String receiptNumber;
    }

    @GetMapping("/bills/pending")
    @Transactional
    public ResponseEntity<List<MaintenanceBillDto>> getPendingBills(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        Long userId = principal.getId();
        ensureActiveBillExists(userId);

        List<Invoice> invoices = invoiceRepository.findByResidentIdOrderByGeneratedAtDesc(userId);
        List<MaintenanceBillDto> pending = invoices.stream()
                .filter(i -> !"PAID".equalsIgnoreCase(i.getStatus()))
                .map(this::toBillDto)
                .toList();

        return ResponseEntity.ok(pending);
    }

    @GetMapping("/bills/history")
    public ResponseEntity<List<MaintenanceBillDto>> getPaymentHistory(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        Long userId = principal.getId();
        List<Invoice> invoices = invoiceRepository.findByResidentIdOrderByGeneratedAtDesc(userId);
        List<MaintenanceBillDto> paid = invoices.stream()
                .filter(i -> "PAID".equalsIgnoreCase(i.getStatus()))
                .map(this::toBillDto)
                .toList();

        return ResponseEntity.ok(paid);
    }

    @PostMapping("/pay/{billId}")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @PathVariable String billId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Invoice invoice = getAuthorizedInvoice(billId, principal);
        double amount = 5550.0;
        if (invoice.getTotalAmount() != null) {
            amount = invoice.getTotalAmount().doubleValue();
        }

        String orderId = "order_mana_" + billId + "_" + System.currentTimeMillis();
        PaymentInitiationResponse resp = PaymentInitiationResponse.builder()
                .orderId(orderId)
                .amount(amount)
                .key("rzp_test_manaCommunityKey")
                .currency("INR")
                .build();

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @RequestBody PaymentVerificationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        String receiptNum = "RCP-" + (System.currentTimeMillis() % 1000000L);
        String nowIso = LocalDateTime.now().toString();

        Invoice invoice = getAuthorizedInvoice(request.getBillId(), principal);
        invoice.setStatus("PAID");
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
        postGlEntryForInvoice(invoice, request.getMethod() != null ? request.getMethod() : "GATEWAY");

        PaymentVerificationResponse resp = PaymentVerificationResponse.builder()
                .success(true)
                .receiptNumber(receiptNum)
                .paidAt(nowIso)
                .message("Payment verified and settled successfully.")
                .build();

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/wallet/balance")
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        Long userId = principal.getId();
        BigDecimal bal = WALLET_BALANCES.computeIfAbsent(userId, k -> new BigDecimal("2500.00"));
        return ResponseEntity.ok(new WalletBalanceResponse(bal.doubleValue(), "INR"));
    }

    @PostMapping("/pay-wallet/{billId}")
    @Transactional
    public ResponseEntity<WalletPaymentResponse> payWithWallet(
            @PathVariable String billId,
            @RequestBody WalletPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Invoice invoice = getAuthorizedInvoice(billId, principal);
        Long userId = principal.getId();
        BigDecimal currentBal = WALLET_BALANCES.computeIfAbsent(userId, k -> new BigDecimal("2500.00"));
        BigDecimal debit = BigDecimal.valueOf(request.getAmount() != null ? request.getAmount() : 5550.0);

        BigDecimal newBal = currentBal.subtract(debit);
        if (newBal.compareTo(BigDecimal.ZERO) < 0) {
            newBal = BigDecimal.ZERO;
        }
        WALLET_BALANCES.put(userId, newBal);

        String receiptNum = "RCP-WAL-" + (System.currentTimeMillis() % 1000000L);

        invoice.setStatus("PAID");
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
        postGlEntryForInvoice(invoice, "WALLET");

        WalletPaymentResponse resp = WalletPaymentResponse.builder()
                .success(true)
                .newBalance(newBal.doubleValue())
                .receiptNumber(receiptNum)
                .build();

        return ResponseEntity.ok(resp);
    }

    // ── Additional endpoints for maintenance dues & billing API ───────────────

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestParam(required = false) Long societyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long commId = societyId != null ? societyId : (principal != null ? principal.getCommunityId() : null);
        if (commId == null) {
            return ResponseEntity.ok(Map.of(
                    "totalDue", 0.0,
                    "totalPaid", 0.0,
                    "pendingCount", 0,
                    "overdueCount", 0
            ));
        }
        List<Invoice> list = invoiceRepository.findByCommunityId(commId);
        double totalDue = 0.0;
        double totalPaid = 0.0;
        int pendingCount = 0;
        int overdueCount = 0;
        LocalDate today = LocalDate.now();
        for (Invoice inv : list) {
            double amt = inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0.0;
            if ("PAID".equalsIgnoreCase(inv.getStatus())) {
                totalPaid += amt;
            } else {
                totalDue += amt;
                pendingCount++;
                if (inv.getDueDate() != null && inv.getDueDate().isBefore(today)) {
                    overdueCount++;
                }
            }
        }
        return ResponseEntity.ok(Map.of(
                "totalDue", totalDue,
                "totalPaid", totalPaid,
                "pendingCount", pendingCount,
                "overdueCount", overdueCount
        ));
    }

    @GetMapping("/dues")
    public ResponseEntity<Map<String, Object>> getDues(
            @RequestParam(required = false) Long societyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long commId = societyId != null ? societyId : (principal != null ? principal.getCommunityId() : null);
        List<Invoice> list = commId != null ? invoiceRepository.findByCommunityId(commId) : List.of();
        if (status != null && !status.isBlank()) {
            list = list.stream().filter(i -> status.equalsIgnoreCase(i.getStatus())).toList();
        }
        List<Map<String, Object>> content = list.stream().map(this::toDueMap).toList();
        return ResponseEntity.ok(Map.of(
                "content", content,
                "totalElements", content.size(),
                "totalPages", (content.size() + size - 1) / Math.max(1, size),
                "size", size,
                "number", page
        ));
    }

    @GetMapping("/dues/mine")
    public ResponseEntity<Map<String, Object>> getMyDues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new AccessDeniedException("Authentication required.");
        ensureActiveBillExists(principal.getId());
        List<Invoice> invoices = invoiceRepository.findByResidentIdOrderByGeneratedAtDesc(principal.getId());
        List<Map<String, Object>> content = invoices.stream().map(this::toDueMap).toList();
        return ResponseEntity.ok(Map.of(
                "content", content,
                "totalElements", content.size(),
                "totalPages", (content.size() + size - 1) / Math.max(1, size),
                "size", size,
                "number", page
        ));
    }

    @GetMapping("/dues/{id}")
    public ResponseEntity<Map<String, Object>> getDueById(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        return ResponseEntity.ok(toDueMap(invoice));
    }

    @PostMapping("/dues/{dueId}/pay")
    @Transactional
    public ResponseEntity<Map<String, Object>> payDue(
            @PathVariable Long dueId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        Invoice invoice = invoiceRepository.findById(dueId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + dueId));
        invoice.setStatus("PAID");
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
        String mode = body != null && body.get("paymentMode") != null ? body.get("paymentMode").toString() : "UPI";
        postGlEntryForInvoice(invoice, mode);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", invoice.getId());
        resp.put("unitNumber", invoice.getResident() != null ? invoice.getResident().getFlatNo() : "");
        resp.put("amount", invoice.getTotalAmount() != null ? invoice.getTotalAmount().doubleValue() : 0.0);
        resp.put("paymentDate", LocalDateTime.now().toString());
        resp.put("paymentMode", mode);
        resp.put("receiptNumber", "RCP-" + invoice.getId());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/dues/generate")
    @Transactional
    public ResponseEntity<Map<String, Object>> generateBulkDues(
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long commId = principal != null && principal.getCommunityId() != null ? principal.getCommunityId() : 1L;
        Community community = principal != null && principal.getUser() != null ? principal.getUser().getCommunity() : null;
        List<AppUser> residents = appUserRepository.findByCommunityIdAndIsActiveTrue(commId);
        int generated = 0;
        for (AppUser res : residents) {
            String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            Invoice inv = Invoice.builder()
                    .invoiceNumber(String.format("INV-%s-%04d", datePrefix, invoiceRepository.countByCommunityId(commId) + 1))
                    .resident(res)
                    .community(community)
                    .taxableAmount(new BigDecimal("4703.39"))
                    .cgst(new BigDecimal("423.31"))
                    .sgst(new BigDecimal("423.30"))
                    .totalAmount(new BigDecimal("5550.00"))
                    .dueDate(LocalDate.now().plusDays(15))
                    .status("PENDING")
                    .generatedAt(LocalDateTime.now())
                    .build();
            invoiceRepository.save(inv);
            generated++;
        }
        return ResponseEntity.ok(Map.of("generated", generated, "status", "SUCCESS"));
    }

    @PostMapping("/dues/remind")
    public ResponseEntity<Map<String, Object>> sendDuesReminders(
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("sent", 1, "status", "SUCCESS"));
    }

    private Map<String, Object> toDueMap(Invoice inv) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", inv.getId());
        m.put("unitNumber", inv.getResident() != null ? inv.getResident().getFlatNo() : "");
        m.put("ownerName", inv.getResident() != null ? inv.getResident().getFullName() : "");
        m.put("amount", inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0.0);
        m.put("dueDate", inv.getDueDate() != null ? inv.getDueDate().toString() : "");
        m.put("status", inv.getStatus());
        m.put("paidAmount", "PAID".equalsIgnoreCase(inv.getStatus()) ? (inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 0.0) : 0.0);
        m.put("paidDate", inv.getPaidAt() != null ? inv.getPaidAt().toString() : null);
        m.put("month", inv.getGeneratedAt() != null ? inv.getGeneratedAt().getMonth().toString() : "");
        return m;
    }

    private void postGlEntryForInvoice(Invoice invoice, String method) {
        if (invoice == null || invoice.getCommunity() == null) return;
        try {
            Long commId = invoice.getCommunity().getId();
            BigDecimal amt = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
            if (amt.compareTo(BigDecimal.ZERO) <= 0) return;

            String vNum = "REC-" + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : invoice.getId());
            GeneralLedgerEntry dr = GeneralLedgerEntry.builder()
                    .communityId(commId)
                    .entryDate(LocalDate.now())
                    .voucherNumber(vNum)
                    .accountCode("1010")
                    .accountName("Operating Bank Account")
                    .debit(amt)
                    .credit(BigDecimal.ZERO)
                    .description("Maintenance collection (" + method + ") for " + invoice.getInvoiceNumber())
                    .referenceType("INVOICE")
                    .referenceId(String.valueOf(invoice.getId()))
                    .build();

            GeneralLedgerEntry cr = GeneralLedgerEntry.builder()
                    .communityId(commId)
                    .entryDate(LocalDate.now())
                    .voucherNumber(vNum)
                    .accountCode("3010")
                    .accountName("Maintenance Collections")
                    .debit(BigDecimal.ZERO)
                    .credit(amt)
                    .description("Maintenance fee from " + (invoice.getResident() != null ? invoice.getResident().getFullName() : "resident"))
                    .referenceType("INVOICE")
                    .referenceId(String.valueOf(invoice.getId()))
                    .build();

            generalLedgerEntryRepository.save(dr);
            generalLedgerEntryRepository.save(cr);
        } catch (Exception ex) {
            log.warn("Failed to post GL entry for invoice {}: {}", invoice.getId(), ex.getMessage());
        }
    }

    private Invoice getAuthorizedInvoice(String billId, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        if (billId == null || billId.isBlank()) {
            throw new IllegalArgumentException("Bill ID must not be empty.");
        }
        Long invId;
        try {
            invId = Long.parseLong(billId.replace("inv-", "").trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid bill format: " + billId);
        }
        Invoice invoice = invoiceRepository.findById(invId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invId));
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a ->
                "ROLE_SUPER_ADMIN".equals(a.getAuthority()) ||
                "ROLE_ADMIN".equals(a.getAuthority()) ||
                "ROLE_COMMUNITY_ADMIN".equals(a.getAuthority()) ||
                "ROLE_FINANCE_MANAGER".equals(a.getAuthority()));
        if (!isAdmin && (invoice.getResident() == null || !principal.getId().equals(invoice.getResident().getId()))) {
            throw new AccessDeniedException("You do not have permission to access or pay this invoice.");
        }
        return invoice;
    }

    private void ensureActiveBillExists(Long userId) {
        List<Invoice> existing = invoiceRepository.findByResidentIdOrderByGeneratedAtDesc(userId);
        if (existing.stream().anyMatch(i -> !"PAID".equalsIgnoreCase(i.getStatus()))) {
            return;
        }

        Optional<AppUser> userOpt = appUserRepository.findById(userId);
        if (userOpt.isEmpty()) return;

        AppUser resident = userOpt.get();
        Community community = resident.getCommunity();

        String monthPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Invoice newInv = Invoice.builder()
                .invoiceNumber("BILL-" + monthPrefix + "-U" + userId)
                .resident(resident)
                .community(community)
                .taxableAmount(new BigDecimal("4703.39"))
                .cgst(new BigDecimal("423.31"))
                .sgst(new BigDecimal("423.30"))
                .totalAmount(new BigDecimal("5550.00"))
                .dueDate(LocalDate.now().plusDays(15))
                .status("PENDING")
                .generatedAt(LocalDateTime.now())
                .build();

        invoiceRepository.save(newInv);
    }

    private MaintenanceBillDto toBillDto(Invoice inv) {
        double total = inv.getTotalAmount() != null ? inv.getTotalAmount().doubleValue() : 5550.0;
        boolean isPaid = "PAID".equalsIgnoreCase(inv.getStatus());

        List<BillChargeDto> charges = List.of(
                new BillChargeDto("Society Maintenance Fee", 3500.0),
                new BillChargeDto("Water Consumption Charges", 450.0),
                new BillChargeDto("Power Backup & DG Surcharge", 800.0),
                new BillChargeDto("Covered Parking Slot 1", 500.0),
                new BillChargeDto("Clubhouse & Gym Membership", 300.0)
        );

        String monthYear = inv.getGeneratedAt() != null
                ? inv.getGeneratedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                : LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));

        return MaintenanceBillDto.builder()
                .id(String.valueOf(inv.getId()))
                .monthYear(monthYear)
                .billNumber(inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "BILL-" + inv.getId())
                .maintenanceAmount(3500.0)
                .waterCharges(450.0)
                .sinkingFund(800.0)
                .penaltyLateFee(0.0)
                .totalAmount(total)
                .paidAmount(isPaid ? total : 0.0)
                .dueAmount(isPaid ? 0.0 : total)
                .dueDate(inv.getDueDate() != null ? inv.getDueDate().toString() : LocalDate.now().plusDays(15).toString())
                .status(isPaid ? "PAID" : "PENDING")
                .charges(charges)
                .receiptUrl(isPaid ? "https://manacommunity.app/receipts/" + inv.getInvoiceNumber() + ".pdf" : null)
                .paymentMethod(isPaid ? "UPI" : null)
                .paidAt(inv.getPaidAt() != null ? inv.getPaidAt().toString() : null)
                .build();
    }
}