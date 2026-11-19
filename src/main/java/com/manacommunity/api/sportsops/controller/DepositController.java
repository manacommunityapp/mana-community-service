package com.manacommunity.api.sportsops.controller;

import com.manacommunity.api.sportsops.entity.SportsNoShowDeposit;
import com.manacommunity.api.sportsops.service.DepositService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sports-ops/deposits")
@RequiredArgsConstructor
public class DepositController {
    private final DepositService service;

    @GetMapping
    public List<SportsNoShowDeposit> list(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listByCommunity(principal.getCommunityId());
    }

    @GetMapping("/my")
    public List<SportsNoShowDeposit> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listByUser(principal.getCommunityId(), principal.getUserId());
    }

    @PostMapping
    public SportsNoShowDeposit hold(@AuthenticationPrincipal UserPrincipal principal,
                                     @RequestBody SportsNoShowDeposit deposit) {
        deposit.setCommunityId(principal.getCommunityId());
        return service.hold(deposit);
    }

    @PutMapping("/{id}/release")
    public SportsNoShowDeposit release(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        return service.release(principal.getCommunityId(), id, body.get("reason"));
    }

    @PutMapping("/{id}/forfeit")
    public SportsNoShowDeposit forfeit(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        return service.forfeit(principal.getCommunityId(), id, body.get("reason"));
    }
}
