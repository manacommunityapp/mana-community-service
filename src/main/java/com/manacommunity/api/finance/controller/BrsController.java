package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.entity.BrsLine;
import com.manacommunity.api.finance.entity.BrsSession;
import com.manacommunity.api.finance.repository.BrsLineRepository;
import com.manacommunity.api.finance.repository.BrsSessionRepository;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.finance.repository.ChequePaymentRecordRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/brs")
@RequiredArgsConstructor
public class BrsController {

    private final BrsSessionRepository sessionRepo;
    private final BrsLineRepository lineRepo;
    private final InvoiceRepository invoiceRepo;
    private final ChequePaymentRecordRepository chequeRepo;

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<BrsSession>> listSessions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(sessionRepo.findByCommunityIdOrderByCreatedAtDesc(
                principal.getCommunityId()));
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BrsSession> createSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long communityId = principal.getCommunityId();
        BrsSession session = BrsSession.builder()
                .communityId(communityId)
                .bankAccountName((String) body.get("bankAccountName"))
                .statementMonth((String) body.get("statementMonth"))
                .statementFromDate(body.get("statementFromDate") != null
                        ? LocalDate.parse(body.get("statementFromDate").toString()) : null)
                .statementToDate(body.get("statementToDate") != null
                        ? LocalDate.parse(body.get("statementToDate").toString()) : null)
                .openingBalance(body.get("openingBalance") != null
                        ? new BigDecimal(body.get("openingBalance").toString()) : BigDecimal.ZERO)
                .closingBalance(body.get("closingBalance") != null
                        ? new BigDecimal(body.get("closingBalance").toString()) : BigDecimal.ZERO)
                .build();
        return ResponseEntity.ok(sessionRepo.save(session));
    }

    @GetMapping("/sessions/{id}/lines")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<BrsLine>> getLines(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        BrsSession session = getSession(id, principal.getCommunityId());
        return ResponseEntity.ok(lineRepo.findBySessionIdOrderByValueDateAsc(session.getId()));
    }

    @PostMapping("/sessions/{id}/lines/upload")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Integer>> uploadLines(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> lines) {
        BrsSession session = getSession(id, principal.getCommunityId());
        int count = 0;
        for (Map<String, Object> lineData : lines) {
            BrsLine line = BrsLine.builder()
                    .session(session)
                    .valueDate(LocalDate.parse(lineData.get("valueDate").toString()))
                    .description((String) lineData.getOrDefault("description", ""))
                    .creditAmount(lineData.get("creditAmount") != null
                            ? new BigDecimal(lineData.get("creditAmount").toString()) : null)
                    .debitAmount(lineData.get("debitAmount") != null
                            ? new BigDecimal(lineData.get("debitAmount").toString()) : null)
                    .utrReference((String) lineData.get("utrReference"))
                    .matchStatus("UNMATCHED")
                    .build();
            lineRepo.save(line);
            count++;
        }
        return ResponseEntity.ok(Map.of("uploaded", count));
    }

    @PostMapping("/sessions/{id}/auto-match")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Integer>> autoMatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        BrsSession session = getSession(id, principal.getCommunityId());
        List<BrsLine> unmatchedLines = lineRepo.findBySessionIdOrderByValueDateAsc(session.getId())
                .stream().filter(l -> "UNMATCHED".equals(l.getMatchStatus())).toList();

        List<Invoice> paidInvoices = invoiceRepo.findByCommunityIdOrderByGeneratedAtDesc(
                session.getCommunityId(), org.springframework.data.domain.PageRequest.of(0, 500)).getContent();

        int matched = 0;
        for (BrsLine line : unmatchedLines) {
            if (line.getCreditAmount() != null) {
                boolean found = paidInvoices.stream().anyMatch(inv ->
                        "PAID".equals(inv.getStatus())
                                && inv.getTotalAmount().compareTo(line.getCreditAmount()) == 0);
                if (found) {
                    line.setMatchStatus("MATCHED_INVOICE");
                    lineRepo.save(line);
                    matched++;
                    continue;
                }
                // Check cleared cheques
                boolean chequeMatch = chequeRepo.findByCommunityIdAndStatusOrderByCreatedAtDesc(
                        session.getCommunityId(), "CLEARED")
                        .stream().anyMatch(c -> c.getAmount().compareTo(line.getCreditAmount()) == 0);
                if (chequeMatch) {
                    line.setMatchStatus("MATCHED_PAYMENT");
                    lineRepo.save(line);
                    matched++;
                }
            }
        }
        return ResponseEntity.ok(Map.of("matched", matched, "total", unmatchedLines.size()));
    }

    @PutMapping("/sessions/{id}/lines/{lineId}/match")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BrsLine> manualMatch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long lineId,
            @RequestBody Map<String, Object> body) {
        getSession(id, principal.getCommunityId());
        BrsLine line = lineRepo.findById(lineId)
                .orElseThrow(() -> new IllegalArgumentException("Line not found: " + lineId));
        line.setMatchedEntityId(body.get("matchedEntityId") != null
                ? Long.parseLong(body.get("matchedEntityId").toString()) : null);
        line.setMatchedEntityType((String) body.get("matchedEntityType"));
        line.setManualNote((String) body.get("manualNote"));
        line.setMatchStatus("MATCHED_INVOICE");
        return ResponseEntity.ok(lineRepo.save(line));
    }

    @PutMapping("/sessions/{id}/lines/{lineId}/suspense")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BrsLine> markSuspense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long lineId,
            @RequestBody(required = false) Map<String, String> body) {
        getSession(id, principal.getCommunityId());
        BrsLine line = lineRepo.findById(lineId)
                .orElseThrow(() -> new IllegalArgumentException("Line not found: " + lineId));
        line.setMatchStatus("SUSPENSE");
        if (body != null) {
            line.setManualNote(body.get("manualNote"));
        }
        return ResponseEntity.ok(lineRepo.save(line));
    }

    @PostMapping("/sessions/{id}/lock")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BrsSession> lock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        BrsSession session = getSession(id, principal.getCommunityId());
        if ("LOCKED".equals(session.getStatus())) {
            throw new IllegalStateException("Session already locked");
        }
        session.setStatus("LOCKED");
        session.setLockedAt(LocalDateTime.now());
        return ResponseEntity.ok(sessionRepo.save(session));
    }

    @GetMapping("/sessions/{id}/summary")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> summary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        BrsSession session = getSession(id, principal.getCommunityId());
        long matched = lineRepo.countBySessionIdAndMatchStatus(session.getId(), "MATCHED_INVOICE")
                + lineRepo.countBySessionIdAndMatchStatus(session.getId(), "MATCHED_PAYMENT")
                + lineRepo.countBySessionIdAndMatchStatus(session.getId(), "MATCHED_VENDOR");
        long unmatched = lineRepo.countBySessionIdAndMatchStatus(session.getId(), "UNMATCHED");
        long suspense = lineRepo.countBySessionIdAndMatchStatus(session.getId(), "SUSPENSE");
        BigDecimal credits = lineRepo.sumCreditsBySessionId(session.getId());
        BigDecimal debits = lineRepo.sumDebitsBySessionId(session.getId());

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalMatched", matched);
        summary.put("totalUnmatched", unmatched);
        summary.put("totalSuspense", suspense);
        summary.put("totalCredits", credits);
        summary.put("totalDebits", debits);
        summary.put("openingBalance", session.getOpeningBalance());
        summary.put("closingBalance", session.getClosingBalance());
        summary.put("status", session.getStatus());
        return ResponseEntity.ok(summary);
    }

    private BrsSession getSession(Long id, Long communityId) {
        return sessionRepo.findById(id)
                .filter(s -> s.getCommunityId().equals(communityId))
                .orElseThrow(() -> new IllegalArgumentException("BRS session not found: " + id));
    }
}
