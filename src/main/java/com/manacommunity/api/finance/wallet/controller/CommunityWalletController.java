package com.manacommunity.api.finance.wallet.controller;

import com.manacommunity.api.finance.wallet.dto.WalletDtos.*;
import com.manacommunity.api.finance.wallet.entity.*;
import com.manacommunity.api.finance.wallet.service.CommunityWalletService;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class CommunityWalletController {

    private final CommunityWalletService walletService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal UserPrincipal principal
) {
        Long communityId = principal.getCommunityId();
        AppUser user = loggedInUserService.resolve(principal);
        CommunityResidentWallet w = walletService.getOrCreateWallet(user.getId(), communityId);
        return ResponseEntity.ok(ApiResponse.ok(toWalletResponse(w)));
    }

    @GetMapping("/my/transactions")
    public ResponseEntity<ApiResponse<Page<CommunityWalletTransaction>>> getMyTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        Long communityId = principal.getCommunityId();
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ApiResponse.ok(
                walletService.getTransactionHistory(user.getId(), communityId, pageable)));
    }

    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<TopupInitResult>> initiateTopup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody TopupRequest req) {
        Long communityId = principal.getCommunityId();
        AppUser user = loggedInUserService.resolve(principal);
        TopupInitResult result = walletService.initiateTopup(user.getId(), communityId,
                req.amount(), req.paymentMethod());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Gateway callback: credits the wallet with the amount recorded on the PENDING
     * top-up row named by {@code externalTxnRef}.
     *
     * <p>The client cannot influence the credited amount, and the gateway signature
     * must verify (see {@code CommunityWalletService#requireVerifiedGatewaySignature}).
     * A reference can be consumed exactly once — a duplicate callback is reported as
     * already processed rather than crediting twice or failing with a 500.
     */
    @PostMapping("/topup/confirm")
    public ResponseEntity<ApiResponse<CommunityWalletTransaction>> confirmTopup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ConfirmTopupPayload req) {
        Long communityId = requireCommunityId(principal);
        AppUser user = loggedInUserService.resolve(principal);
        try {
            CommunityWalletTransaction txn = walletService.confirmTopup(
                    user.getId(), communityId, req.externalTxnRef(),
                    req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
            return ResponseEntity.ok(ApiResponse.ok(txn, "Wallet credited"));
        } catch (DataIntegrityViolationException e) {
            // Unique index on the top-up reference (V503) fired: a concurrent or
            // replayed callback already consumed this reference.
            log.info("Duplicate top-up confirmation for ref={} — already processed", req.externalTxnRef());
            return walletService.findTopupByReference(req.externalTxnRef())
                    .map(existing -> ResponseEntity.ok(
                            ApiResponse.ok(existing, "Top-up already processed")))
                    .orElseThrow(() -> new IllegalStateException(
                            "This top-up has already been processed."));
        }
    }

    /**
     * Body of {@code POST /api/wallet/topup/confirm}.
     *
     * <p>Replaces {@code WalletDtos.ConfirmTopupRequest}, which carries no gateway
     * fields; the signed triplet is required because the wallet is credited here.
     */
    public record ConfirmTopupPayload(
            String externalTxnRef,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {}

    @PostMapping("/pay/invoice")
    public ResponseEntity<ApiResponse<CommunityWalletTransaction>> payInvoice(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PayInvoiceRequest req) {
        Long communityId = principal.getCommunityId();
        AppUser user = loggedInUserService.resolve(principal);
        CommunityWalletTransaction txn = walletService.payInvoice(
                user.getId(), communityId, req.invoiceId(), req.amount());
        return ResponseEntity.ok(ApiResponse.ok(txn, "Invoice paid"));
    }

    @PostMapping("/pay/booking")
    public ResponseEntity<ApiResponse<CommunityWalletTransaction>> payBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PayBookingRequest req) {
        Long communityId = principal.getCommunityId();
        AppUser user = loggedInUserService.resolve(principal);
        CommunityWalletTransaction txn = walletService.payBooking(
                user.getId(), communityId, req.bookingId(), req.amount());
        return ResponseEntity.ok(ApiResponse.ok(txn, "Booking paid"));
    }

    @PostMapping("/pay/expense")
    public ResponseEntity<ApiResponse<CommunityWalletTransaction>> settleExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SettleExpenseRequest req) {
        Long communityId = principal.getCommunityId();
        AppUser user = loggedInUserService.resolve(principal);
        CommunityWalletTransaction txn = walletService.settleGroupExpense(
                user.getId(), communityId, req.expenseId(), req.amount());
        return ResponseEntity.ok(ApiResponse.ok(txn, "Expense settled"));
    }

    @PostMapping("/credit-lines")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CommunityWalletCreditLine>> approveCreditLine(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ApproveCreditLineRequest req) {
        Long communityId = principal.getCommunityId();
        AppUser admin = loggedInUserService.resolve(principal);
        CommunityWalletCreditLine line = walletService.approveCreditLine(
                req.walletId(), communityId, req.amount(), admin.getId(), req.repaymentDays());
        return ResponseEntity.ok(ApiResponse.ok(line, "Credit line approved"));
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<CommunityWalletTransaction>>> getCommunityLedger(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            Pageable pageable) {
        Long communityId = principal.getCommunityId();
        return ResponseEntity.ok(ApiResponse.ok(
                walletService.getCommunityLedger(communityId, type, pageable)));
    }

    @PostMapping("/refund/{txnId}")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CommunityWalletTransaction>> refund(
            @PathVariable Long txnId,
            @RequestBody RefundRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        CommunityWalletTransaction txn = walletService.refund(
                txnId, requireCommunityId(principal), req.reason());
        return ResponseEntity.ok(ApiResponse.ok(txn, "Refund issued"));
    }

    private Long requireCommunityId(UserPrincipal principal) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        if (communityId == null) {
            throw new AccessDeniedException("No community is associated with this account.");
        }
        return communityId;
    }

    private WalletResponse toWalletResponse(CommunityResidentWallet w) {
        return new WalletResponse(w.getId(), w.getResidentId(), w.getCommunityId(),
                w.getBalance(), w.getCreditLimit(), w.getCurrency(),
                w.getStatus(), w.getUpiVirtualId());
    }
}
