package com.cpn.web;

import com.cpn.application.referrals.ReferralsService;
import com.cpn.domain.referrals.model.Referral;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cpn/referrals")
@RequiredArgsConstructor
public class ReferralsController {

    private final ReferralsService referralsService;

    @GetMapping
    public ResponseEntity<List<Referral>> getOpenReferrals() {
        return ResponseEntity.ok(referralsService.getAllOpenReferrals());
    }

    @PostMapping
    public ResponseEntity<Referral> createReferral(@RequestBody Referral referral) {
        return ResponseEntity.ok(referralsService.createReferral(referral));
    }
}
