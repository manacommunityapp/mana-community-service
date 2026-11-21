package com.manacommunity.api.finance.wallet.controller;

import com.manacommunity.api.finance.wallet.dto.WalletDtos.*;
import com.manacommunity.api.finance.wallet.entity.*;
import com.manacommunity.api.finance.wallet.service.CommunityWalletService;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class CommunityWalletController {

    private final CommunityWalletService walletService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId) {
        AppUser user = loggedInUserService.resolve(principal);
        CommunityResidentWallet w = walletService.getOrCreateWallet(user.getId(), communityId);
        return ResponseEntity.ok(ApiResponse.ok(toWalletResponse(w)));
    }

    @GetMapping("/my/transactions")
    public ResponseEntity<ApiResponse<Page<CommunityWalletTransaction>>> getMyTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId,
            Pageable pageable) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ApiResponse.ok(
                walletService.getTransactionHistory(user.getId(), communityId, pageable)));
    }

    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<TopupInitResult>> initiateTopup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId,
            @RequestBody TopupRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        TopupInitResult result = walletService.initiateTopup(user.getId(), communityId,
                req.amount(), req.paymentMethod());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/topup/confirm")
    public ResponseEntity<ApiResponse<CommunityWalletTransaction>> confirmTopup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId,
            @RequestBody ConfirmTopupRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        // Amount from gateway — stub uses ₹100 for testing
        java.math.BigDecimal amount = java.math.BigDecimal.valueOf(100);
        CommunityWalletTransaction txn = walletService.confirmTopup(
                user.getId(), communityId, req.externalTxnRef(), amount);
        return ResponseEntity.ok(ApiResponse.ok(txn, "Wallet credited"));
    }

    @PostMapping("/pay/invoice")
    public ResponseEntity<ApiResponse<CommunityWalletTransaction>> payInvoice(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId,
            @RequestBody PayInvoiceRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        CommunityWalletTransaction txn = walletService.payInvoice(
                user.getId(), communityId, req.invoiceId(), req.amount());
        return ResponseEntity.ok(ApiResponse.ok(txn, "Invoice paid"));
    }

    @PostMapping("/pay/booking")
    public ResponseEntity<ApiResponse<CommunityWalletTransaction>> payBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId,
            @RequestBody PayBookingRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        CommunityWalletTransaction txn = walletService.payBooking(
                user.getId(), communityId, req.bookingId(), req.amount());
        return ResponseEntity.ok(ApiResponse.ok(txn, "Booking paid"));
    }

    @PostMapping("/pay/expense")
    public ResponseEntity<ApiResponse<CommunityWalletTransaction>> settleExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId,
            @RequestBody SettleExpenseRequest req) {
        AppUser user = loggedInUserService.resolve(principal);
        CommunityWalletTransaction txn = walletService.settleGroupExpense(
                user.getId(), communityId, req.expenseId(), req.amount());
        return ResponseEntity.ok(ApiResponse.ok(txn, "Expense settled"));
    }

    @PostMapping("/credit-lines")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CommunityWalletCreditLine>> approveCreditLine(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long communityId,
            @RequestBody ApproveCreditLineRequest req) {
        AppUser admin = loggedInUserService.resolve(principal);
        CommunityWalletCreditLine line = walletService.approveCreditLine(
                req.walletId(), communityId, req.amount(), admin.getId(), req.repaymentDays());
        return ResponseEntity.ok(ApiResponse.ok(line, "Credit line approved"));
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<CommunityWalletTransaction>>> getCommunityLedger(
            @RequestParam Long communityId,
            @RequestParam(required = false) String type,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                walletService.getCommunityLedger(communityId, type, pageable)));
    }

    @PostMapping("/refund/{txnId}")
    @PreAuthorize("hasAnyRole('FACILITY_MANAGER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CommunityWalletTransaction>> refund(
            @PathVariable Long txnId,
            @RequestBody RefundRequest req) {
        CommunityWalletTransaction txn = walletService.refund(txnId, req.reason());
        return ResponseEntity.ok(ApiResponse.ok(txn, "Refund issued"));
    }

    private WalletResponse toWalletResponse(CommunityResidentWallet w) {
        return new WalletResponse(w.getId(), w.getResidentId(), w.getCommunityId(),
                w.getBalance(), w.getCreditLimit(), w.getCurrency(),
                w.getStatus(), w.getUpiVirtualId());
    }
}
