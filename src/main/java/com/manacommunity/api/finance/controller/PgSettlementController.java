package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.entity.PaymentGatewaySettlement;
import com.manacommunity.api.finance.repository.PaymentGatewaySettlementRepository;
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
@RequestMapping("/api/finance/pg-settlements")
@RequiredArgsConstructor
public class PgSettlementController {

    private final PaymentGatewaySettlementRepository settlementRepo;

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<PaymentGatewaySettlement>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {
        Long communityId = principal.getCommunityId();
        List<PaymentGatewaySettlement> result = status != null && !status.isBlank()
                ? settlementRepo.findByCommunityIdAndStatusOrderBySettlementDateDesc(communityId, status)
                : settlementRepo.findByCommunityIdOrderBySettlementDateDesc(communityId);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PaymentGatewaySettlement> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long communityId = principal.getCommunityId();
        PaymentGatewaySettlement settlement = PaymentGatewaySettlement.builder()
                .communityId(communityId)
                .settlementDate(body.get("settlementDate") != null
                        ? LocalDate.parse(body.get("settlementDate").toString()) : LocalDate.now())
                .utrNumber((String) body.get("utrNumber"))
                .grossAmount(new BigDecimal(body.get("grossAmount").toString()))
                .mdrAmount(body.get("mdrAmount") != null
                        ? new BigDecimal(body.get("mdrAmount").toString()) : BigDecimal.ZERO)
                .gstOnMdr(body.get("gstOnMdr") != null
                        ? new BigDecimal(body.get("gstOnMdr").toString()) : BigDecimal.ZERO)
                .netAmount(new BigDecimal(body.get("netAmount").toString()))
                .transactionCount(body.get("transactionCount") != null
                        ? Integer.parseInt(body.get("transactionCount").toString()) : 0)
                .bankAccount((String) body.get("bankAccount"))
                .remarks((String) body.get("remarks"))
                .status("PENDING")
                .build();
        return ResponseEntity.ok(settlementRepo.save(settlement));
    }

    @PutMapping("/{id}/reconcile")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PaymentGatewaySettlement> reconcile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        PaymentGatewaySettlement settlement = settlementRepo.findById(id)
                .filter(s -> s.getCommunityId().equals(principal.getCommunityId()))
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + id));
        settlement.setStatus("RECONCILED");
        return ResponseEntity.ok(settlementRepo.save(settlement));
    }
}
