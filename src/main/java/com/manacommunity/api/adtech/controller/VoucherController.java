package com.manacommunity.api.adtech.controller;

import com.manacommunity.api.adtech.dto.VoucherDtos.*;
import com.manacommunity.api.adtech.service.VoucherService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<SampleVoucherDto>> listAvailableVouchers(
            @RequestParam String societyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(voucherService.listAvailableVouchers(societyId, user));
    }

    @GetMapping("/{voucherId}")
    public ResponseEntity<SampleVoucherDto> getVoucher(
            @PathVariable String voucherId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(voucherService.getVoucher(voucherId, user));
    }

    @PostMapping("/{voucherId}/claim")
    public ResponseEntity<SampleVoucherDto> claimVoucher(
            @PathVariable String voucherId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(voucherService.claimVoucher(voucherId, user));
    }

    @GetMapping("/my-claims")
    public ResponseEntity<List<SampleVoucherDto>> getMyClaimedVouchers(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(voucherService.getMyClaimedVouchers(user));
    }

    @PostMapping("/kiosk/verify")
    public ResponseEntity<KioskScanResultDto> verifyQrOnline(@RequestBody VerifyQrRequestDto request) {
        return ResponseEntity.ok(voucherService.verifyQrOnline(request));
    }

    @PostMapping("/kiosk/sync")
    public ResponseEntity<RedemptionSyncResultDto> syncRedemptions(@RequestBody RedemptionSyncPayloadDto payload) {
        return ResponseEntity.ok(voucherService.syncRedemptions(payload));
    }
}
