package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.entity.HsWorkerPayout;
import com.manacommunity.api.homeservices.service.HsPayoutService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home-services/payouts")
@RequiredArgsConstructor
public class HsPayoutController {

    private final HsPayoutService payoutService;

    @PostMapping
    public ResponseEntity<HsWorkerPayout> create(@RequestBody HsWorkerPayout payout,
            @AuthenticationPrincipal UserPrincipal p) {
        payout.setCommunityId(p.getCommunityId());
        return ResponseEntity.ok(payoutService.create(payout));
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<HsWorkerPayout>> statement(@PathVariable Long workerId) {
        return ResponseEntity.ok(payoutService.workerStatement(workerId));
    }

    @GetMapping("/unpaid")
    public ResponseEntity<List<HsWorkerPayout>> unpaid(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(payoutService.unpaidPayouts(p.getCommunityId()));
    }

    @PutMapping("/{id}/paid")
    public ResponseEntity<HsWorkerPayout> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(payoutService.markPaid(id));
    }
}
