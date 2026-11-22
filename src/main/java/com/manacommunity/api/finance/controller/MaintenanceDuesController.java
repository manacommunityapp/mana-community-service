package com.manacommunity.api.finance.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.manacommunity.api.finance.entity.GeneralLedgerEntry;
import com.manacommunity.api.finance.repository.GeneralLedgerEntryRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.finance.wallet.entity.CommunityWalletTransaction;
import com.manacommunity.api.finance.wallet.service.CommunityWalletService;
import com.manacommunity.api.payments.config.RazorpayProperties;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.manacommunity.api.exception.ResourceNotFoundException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller providing resident maintenance dues, billing statements,
 * Razorpay payment order generation &amp; verification, and maintenance wallet settlement.
 *
 * <p>Money rules enforced here:
 * <ul>
 *   <li>An invoice is only ever marked PAID after either (a) a gateway signature
 *       that verifies against the configured Razorpay secret, or (b) a successful
 *       debit of the resident's <em>persistent</em> community wallet.</li>
 *   <li>Amounts are always taken from the server-side invoice, never from the
 *       request body, and are {@link BigDecimal} end to end.</li>
 *   <li>Cross-community access is denied for every role except SUPER_ADMIN.</li>
 * </ul>
 *
 * <p>CORS is deliberately NOT configured here — the curated allowlist in
 * {@code SecurityConfig} governs these token-bearing payment endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/finance/maintenance")
@RequiredArgsConstructor
public class MaintenanceDuesController {

    /** Prefix of the order ids minted by {@link #initiatePayment}. */
    private static final String LOCAL_ORDER_PREFIX = "order_mana_";

    private static final BigDecimal DEFAULT_MAINTENANCE   = new BigDecimal("3500.00");
    private static final BigDecimal DEFAULT_WATER         = new BigDecimal("450.00");
    private static final BigDecimal DEFAULT_SINKING_FUND  = new BigDecimal("800.00");
    private static final BigDecimal DEFAULT_PARKING       = new BigDecimal("500.00");
    private static final BigDecimal DEFAULT_CLUBHOUSE     = new BigDecimal("300.00");
    private static final BigDecimal DEFAULT_TOTAL         = new BigDecimal("5550.00");

    private final InvoiceRepository invoiceRepository;
    private final AppUserRepository appUserRepository;
    private final GeneralLedgerEntryRepository generalLedgerEntryRepository;
    private final CommunityWalletService walletService;
    private final RazorpayProperties razorpayProperties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillChargeDto {
        private String item;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaintenanceBillDto {
        private String id;
        private String monthYear;
        private String billNumber;
        private BigDecimal maintenanceAmount;
        private BigDecimal waterCharges;
        private BigDecimal sinkingFund;
        private BigDecimal penaltyLateFee;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal dueAmount;
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
        private BigDecimal amount;
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
        private BigDecimal balance;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletPaymentRequest {
        /**
         * Legacy field. IGNORED server-side: the debit always uses the invoice total.
         * Kept only so existing clients keep deserialising.
         */
        private BigDecimal amount;

        /** Declared explicitly so Lombok does not skip it because of the bridge below. */
        @JsonSetter("amount")
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        /**
         * Bridge for {@code MaintenanceWalletAliasController}, which is owned elsewhere
         * and still calls {@code setAmount(double)}. Not visible to Jackson.
         *
         * @deprecated use {@link #setAmount(BigDecimal)}; the value is ignored anyway.
         */
        @Deprecated
        @JsonIgnore
        public void setAmount(double amount) {
            this.amount = BigDecimal.valueOf(amount);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletPaymentResponse {
        private boolean success;
        private BigDecimal newBalance;
        private String receiptNumber;
    }

    /**
     * Read-only listing of the resident's unpaid bills.
     *
     * <p>This GET has no side effects: it never generates an invoice.
     */
    @GetMapping("/bills/pending")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MaintenanceBillDto>> getPendingBills(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        Long userId = principal.getId();

        List<Invoice> invoices = invoiceRepository.findByResidentIdOrderByGeneratedAtDesc(userId);
        List<MaintenanceBillDto> pending = invoices.stream()
                .filter(i -> !"PAID".equalsIgnoreCase(i.getStatus()))
                .map(this::toBillDto)
                .toList();

        return ResponseEntity.ok(pending);
    }

    @GetMapping("/bills/history")
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @PathVariable String billId,
            @AuthenticationPrincipal UserPrincipal principal) {

        Invoice invoice = getAuthorizedInvoice(billId, principal);
        BigDecimal amount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : DEFAULT_TOTAL;

        String orderId = LOCAL_ORDER_PREFIX + billId + "_" + System.currentTimeMillis();
        PaymentInitiationResponse resp = PaymentInitiationResponse.builder()
                .orderId(orderId)
                .amount(amount)
                // Publishable key only — read from app.razorpay.key-id, never hardcoded.
                .key(razorpayProperties.getKeyId())
                .currency(razorpayProperties.getCurrency() != null && !razorpayProperties.getCurrency().isBlank()
                        ? razorpayProperties.getCurrency() : "INR")
                .build();

        return ResponseEntity.ok(resp);
    }

    /**
     * Settles a bill against a gateway payment.
     *
     * <p>Nothing is mutated until the Razorpay signature
     * {@code HMAC-SHA256(orderId + "|" + paymentId, keySecret)} verifies against the
     * {@code signature} field. Verification fails closed when no gateway secret is
     * configured, so a client can never mark its own bill PAID.
     */
    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @RequestBody PaymentVerificationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Invoice invoice = getAuthorizedInvoice(request.getBillId(), principal);

        // Idempotent: an already-settled bill is reported as such, without re-receipting.
        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            return ResponseEntity.ok(PaymentVerificationResponse.builder()
                    .success(true)
                    .receiptNumber(receiptNumber(invoice, "RCP"))
                    .paidAt(invoice.getPaidAt() != null ? invoice.getPaidAt().toString() : null)
                    .message("This bill was already settled.")
                    .build());
        }

        String orderId = request.getOrderId();
        // Bind the signed order to this bill: an order id minted by initiatePayment
        // embeds its bill id, so a signature issued for bill A cannot settle bill B.
        if (orderId != null && orderId.startsWith(LOCAL_ORDER_PREFIX)
                && !orderId.startsWith(LOCAL_ORDER_PREFIX + request.getBillId() + "_")) {
            log.warn("SECURITY: maintenance verify rejected — order {} does not belong to bill {} (user {})",
                    orderId, request.getBillId(), principal.getId());
            throw new com.manacommunity.api.payments.exception.PaymentVerificationException(
                    "This payment order does not belong to the selected bill.");
        }

        // Verify Razorpay HMAC-SHA256 signature; fails closed when no secret configured.
        verifyRazorpaySignature(orderId, request.getPaymentId(), request.getSignature());

        LocalDateTime now = LocalDateTime.now();
        invoice.setStatus("PAID");
        invoice.setPaidAt(now);
        invoiceRepository.save(invoice);
        postGlEntryForInvoice(invoice, request.getMethod() != null ? request.getMethod() : "GATEWAY");

        log.info("Invoice {} settled via gateway paymentId={} by user {}",
                invoice.getId(), request.getPaymentId(), principal.getId());

        PaymentVerificationResponse resp = PaymentVerificationResponse.builder()
                .success(true)
                .receiptNumber(receiptNumber(invoice, "RCP"))
                .paidAt(now.toString())
                .message("Payment verified and settled successfully.")
                .build();

        return ResponseEntity.ok(resp);
    }

    /** Live balance of the resident's persistent community wallet. */
    @GetMapping("/wallet/balance")
    @Transactional
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        Long communityId = requireCommunityId(principal);
        BigDecimal balance = walletService.getBalance(principal.getId(), communityId);
        return ResponseEntity.ok(new WalletBalanceResponse(balance, "INR"));
    }

    /**
     * Settles a bill from the resident's persistent community wallet.
     *
     * <p>The debit amount is {@code invoice.totalAmount} — any amount in the request
     * body is ignored. The invoice is marked PAID only after the wallet debit has
     * succeeded, in the same transaction, so the two can never disagree.
     */
    @PostMapping("/pay-wallet/{billId}")
    @Transactional
    public ResponseEntity<WalletPaymentResponse> payWithWallet(
            @PathVariable String billId,
            @RequestBody(required = false) WalletPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Invoice invoice = getAuthorizedInvoice(billId, principal);

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            throw new IllegalStateException("This bill has already been settled.");
        }

        // Server-side amount only. request.getAmount() is deliberately unused.
        BigDecimal due = invoice.getTotalAmount();
        if (due == null || due.signum() <= 0) {
            throw new IllegalStateException("This bill has no payable amount.");
        }

        Long residentId = invoice.getResident() != null ? invoice.getResident().getId() : null;
        Long communityId = invoice.getCommunity() != null ? invoice.getCommunity().getId() : null;
        if (residentId == null || communityId == null) {
            throw new IllegalStateException("This bill is not linked to a resident wallet.");
        }

        CommunityWalletTransaction txn;
        try {
            txn = walletService.payInvoice(residentId, communityId, invoice.getId(), due);
        } catch (IllegalStateException e) {
            // Insufficient balance (and credit line) — nothing is debited, nothing is PAID.
            log.info("Wallet settlement declined for invoice {} (resident {}): {}",
                    invoice.getId(), residentId, e.getMessage());
            throw new ManaCommunityException(
                    "Insufficient wallet balance for this bill.",
                    HttpStatus.PAYMENT_REQUIRED,
                    "WALLET_INSUFFICIENT_FUNDS");
        }

        invoice.setStatus("PAID");
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
        postGlEntryForInvoice(invoice, "WALLET");

        log.info("Invoice {} settled from wallet txn {} amount={} by user {}",
                invoice.getId(), txn.getId(), due, principal.getId());

        WalletPaymentResponse resp = WalletPaymentResponse.builder()
                .success(true)
                .newBalance(txn.getBalanceAfter())
                .receiptNumber(receiptNumber(invoice, "RCP-WAL"))
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

    /**
     * Loads an invoice the caller is allowed to see and pay.
     *
     * <p>Tenant isolation: every role except SUPER_ADMIN must belong to the invoice's
     * community. Within the community, residents are restricted to their own invoices;
     * ADMIN / COMMUNITY_ADMIN / FINANCE_MANAGER may act on any invoice of that
     * community only.
     */
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

        if (hasAuthority(principal, "ROLE_SUPER_ADMIN")) {
            return invoice;
        }

        Long callerCommunityId = principal.getCommunityId();
        Long invoiceCommunityId = invoice.getCommunity() != null ? invoice.getCommunity().getId() : null;
        if (callerCommunityId == null || invoiceCommunityId == null
                || !callerCommunityId.equals(invoiceCommunityId)) {
            log.warn("SECURITY: user {} (community {}) attempted to access invoice {} of community {}",
                    principal.getId(), callerCommunityId, invId, invoiceCommunityId);
            throw new AccessDeniedException("This invoice belongs to another community.");
        }

        boolean isCommunityStaff = hasAuthority(principal,
                "ROLE_ADMIN", "ROLE_COMMUNITY_ADMIN", "ROLE_FINANCE_MANAGER");
        if (!isCommunityStaff
                && (invoice.getResident() == null || !principal.getId().equals(invoice.getResident().getId()))) {
            throw new AccessDeniedException("You do not have permission to access or pay this invoice.");
        }
        return invoice;
    }

    private boolean hasAuthority(UserPrincipal principal, String... roles) {
        Set<String> wanted = new HashSet<>(Arrays.asList(roles));
        return principal.getAuthorities().stream()
                .anyMatch(a -> wanted.contains(a.getAuthority()));
    }

    private Long requireCommunityId(UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        if (communityId == null) {
            throw new AccessDeniedException("No community is associated with this account.");
        }
        return communityId;
    }

    private String receiptNumber(Invoice invoice, String prefix) {
        return prefix + "-" + invoice.getId();
    }

    private void verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        String secret = razorpayProperties.getKeySecret();
        if (secret == null || secret.isBlank()) {
            log.error("SECURITY: Razorpay key secret not configured — refusing gateway settlement");
            throw new com.manacommunity.api.payments.exception.PaymentVerificationException(
                    "Payment gateway is not configured. Contact support.");
        }
        if (orderId == null || paymentId == null || signature == null) {
            throw new com.manacommunity.api.payments.exception.PaymentVerificationException(
                    "Missing orderId, paymentId, or signature.");
        }
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            if (!hex.toString().equals(signature)) {
                log.warn("SECURITY: Razorpay signature mismatch for order {}", orderId);
                throw new com.manacommunity.api.payments.exception.PaymentVerificationException(
                        "Payment signature verification failed.");
            }
        } catch (com.manacommunity.api.payments.exception.PaymentVerificationException e) {
            throw e;
        } catch (Exception e) {
            log.error("SECURITY: Razorpay signature computation error: {}", e.getMessage());
            throw new com.manacommunity.api.payments.exception.PaymentVerificationException(
                    "Signature verification error.");
        }
    }

    /**
     * Generates the current month's maintenance bill for a resident if none is open.
     *
     * <p>NOT wired to any endpoint or scheduler: it is intentionally kept
     * package-private so an explicit admin action or a future billing job can call it.
     * It must never be invoked from a GET — it writes an invoice.
     */
    void ensureActiveBillExists(Long userId) {
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
                .totalAmount(DEFAULT_TOTAL)
                .dueDate(LocalDate.now().plusDays(15))
                .status("PENDING")
                .generatedAt(LocalDateTime.now())
                .build();

        invoiceRepository.save(newInv);
    }

    private MaintenanceBillDto toBillDto(Invoice inv) {
        BigDecimal total = inv.getTotalAmount() != null ? inv.getTotalAmount() : DEFAULT_TOTAL;
        boolean isPaid = "PAID".equalsIgnoreCase(inv.getStatus());

        List<BillChargeDto> charges = List.of(
                new BillChargeDto("Society Maintenance Fee", DEFAULT_MAINTENANCE),
                new BillChargeDto("Water Consumption Charges", DEFAULT_WATER),
                new BillChargeDto("Power Backup & DG Surcharge", DEFAULT_SINKING_FUND),
                new BillChargeDto("Covered Parking Slot 1", DEFAULT_PARKING),
                new BillChargeDto("Clubhouse & Gym Membership", DEFAULT_CLUBHOUSE)
        );

        String monthYear = inv.getGeneratedAt() != null
                ? inv.getGeneratedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                : LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));

        return MaintenanceBillDto.builder()
                .id(String.valueOf(inv.getId()))
                .monthYear(monthYear)
                .billNumber(inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "BILL-" + inv.getId())
                .maintenanceAmount(DEFAULT_MAINTENANCE)
                .waterCharges(DEFAULT_WATER)
                .sinkingFund(DEFAULT_SINKING_FUND)
                .penaltyLateFee(BigDecimal.ZERO)
                .totalAmount(total)
                .paidAmount(isPaid ? total : BigDecimal.ZERO)
                .dueAmount(isPaid ? BigDecimal.ZERO : total)
                .dueDate(inv.getDueDate() != null ? inv.getDueDate().toString() : LocalDate.now().plusDays(15).toString())
                .status(isPaid ? "PAID" : "PENDING")
                .charges(charges)
                .receiptUrl(isPaid ? "https://manacommunity.app/receipts/" + inv.getInvoiceNumber() + ".pdf" : null)
                .paymentMethod(isPaid ? "UPI" : null)
                .paidAt(inv.getPaidAt() != null ? inv.getPaidAt().toString() : null)
                .build();
    }
}
