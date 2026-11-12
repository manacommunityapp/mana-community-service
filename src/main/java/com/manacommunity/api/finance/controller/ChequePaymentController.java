package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.entity.ChequePaymentRecord;
import com.manacommunity.api.finance.repository.ChequePaymentRecordRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/cheques")
@RequiredArgsConstructor
public class ChequePaymentController {

    private final ChequePaymentRecordRepository chequeRepo;

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<ChequePaymentRecord>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {
        Long communityId = principal.getCommunityId();
        List<ChequePaymentRecord> result = status != null && !status.isBlank()
                ? chequeRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status)
                : chequeRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ChequePaymentRecord> record(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long communityId = principal.getCommunityId();
        ChequePaymentRecord cheque = ChequePaymentRecord.builder()
                .communityId(communityId)
                .residentUserId(body.get("residentUserId") != null
                        ? Long.parseLong(body.get("residentUserId").toString()) : null)
                .flatNo((String) body.get("flatNo"))
                .amount(new BigDecimal(body.get("amount").toString()))
                .chequeNumber((String) body.get("chequeNumber"))
                .bankName((String) body.get("bankName"))
                .chequeDate(body.get("chequeDate") != null
                        ? LocalDate.parse(body.get("chequeDate").toString()) : null)
                .invoiceId(body.get("invoiceId") != null
                        ? Long.parseLong(body.get("invoiceId").toString()) : null)
                .remarks((String) body.get("remarks"))
                .status("IN_HAND")
                .build();
        return ResponseEntity.ok(chequeRepo.save(cheque));
    }

    @PutMapping("/{id}/deposit")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ChequePaymentRecord> deposit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        ChequePaymentRecord cheque = chequeRepo.findById(id)
                .filter(c -> c.getCommunityId().equals(principal.getCommunityId()))
                .orElseThrow(() -> new IllegalArgumentException("Cheque not found: " + id));
        cheque.setStatus("DEPOSITED");
        cheque.setDepositedDate(LocalDate.now());
        return ResponseEntity.ok(chequeRepo.save(cheque));
    }

    @PutMapping("/{id}/clear")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ChequePaymentRecord> clear(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        ChequePaymentRecord cheque = chequeRepo.findById(id)
                .filter(c -> c.getCommunityId().equals(principal.getCommunityId()))
                .orElseThrow(() -> new IllegalArgumentException("Cheque not found: " + id));
        cheque.setStatus("CLEARED");
        cheque.setClearedDate(LocalDate.now());
        if (body != null && body.get("invoiceId") != null) {
            cheque.setInvoiceId(Long.parseLong(body.get("invoiceId").toString()));
        }
        return ResponseEntity.ok(chequeRepo.save(cheque));
    }

    @PutMapping("/{id}/bounce")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ChequePaymentRecord> bounce(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        ChequePaymentRecord cheque = chequeRepo.findById(id)
                .filter(c -> c.getCommunityId().equals(principal.getCommunityId()))
                .orElseThrow(() -> new IllegalArgumentException("Cheque not found: " + id));
        cheque.setStatus("BOUNCED");
        if (body != null) {
            cheque.setRemarks(body.get("remarks"));
        }
        return ResponseEntity.ok(chequeRepo.save(cheque));
    }
}
