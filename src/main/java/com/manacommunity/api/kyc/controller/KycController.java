package com.manacommunity.api.kyc.controller;

import com.manacommunity.api.kyc.dto.KycSubmitRequest;
import com.manacommunity.api.kyc.dto.KycSummaryResponse;
import com.manacommunity.api.kyc.entity.KycDocument;
import com.manacommunity.api.kyc.service.KycService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/status")
    public ResponseEntity<KycSummaryResponse> getStatus(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(kycService.getStatus(user));
    }

    @PostMapping("/submit")
    public ResponseEntity<KycDocument> submit(@RequestBody KycSubmitRequest req,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(kycService.submit(req, user));
    }

    @PutMapping("/documents/{id}")
    public ResponseEntity<KycDocument> resubmit(@PathVariable Long id,
                                                 @RequestBody KycSubmitRequest req,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(kycService.resubmit(id, req, user));
    }
}
