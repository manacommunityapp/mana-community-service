package com.manacommunity.api.finance.controller;

import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class MaintenanceWalletAliasController {

    private final MaintenanceDuesController maintenanceDuesController;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/api/finance/wallet/balance")
    public ResponseEntity<MaintenanceDuesController.WalletBalanceResponse> getWalletBalance(
            @AuthenticationPrincipal UserPrincipal principal) {
        return maintenanceDuesController.getWalletBalance(principal);
    }

    @PostMapping("/api/finance/maintenance/pay-wallet")
    public ResponseEntity<MaintenanceDuesController.WalletPaymentResponse> payWithWalletBody(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Object billIdObj = request.get("billId");
        String billId = billIdObj != null ? billIdObj.toString() : "0";

        MaintenanceDuesController.WalletPaymentRequest walletReq = new MaintenanceDuesController.WalletPaymentRequest();
        Object amountObj = request.get("amount");
        if (amountObj instanceof Number) {
            walletReq.setAmount(((Number) amountObj).doubleValue());
        }
        return maintenanceDuesController.payWithWallet(billId, walletReq, principal);
    }

    @GetMapping("/api/finance/maintenance/bills/{billId}/receipt")
    public ResponseEntity<?> getBillReceipt(
            @PathVariable String billId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            Long invId = Long.parseLong(billId.replace("inv-", ""));
            Optional<Invoice> inv = invoiceRepository.findById(invId);
            if (inv.isPresent()) {
                Invoice invoice = inv.get();
                Map<String, Object> receipt = new LinkedHashMap<>();
                receipt.put("billId", billId);
                receipt.put("invoiceNumber", invoice.getInvoiceNumber());
                receipt.put("totalAmount", invoice.getTotalAmount());
                receipt.put("status", invoice.getStatus());
                receipt.put("paidAt", invoice.getPaidAt() != null ? invoice.getPaidAt().toString() : null);
                receipt.put("receiptNumber", "RCP-" + invoice.getId());
                receipt.put("generatedAt", invoice.getGeneratedAt() != null ? invoice.getGeneratedAt().toString() : null);
                return ResponseEntity.ok(receipt);
            }
        } catch (NumberFormatException ignored) {}
        return ResponseEntity.notFound().build();
    }
}
