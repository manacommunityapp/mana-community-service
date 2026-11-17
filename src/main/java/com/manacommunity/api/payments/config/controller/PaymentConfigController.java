package com.manacommunity.api.payments.config.controller;

import com.manacommunity.api.model.CommunitySettings;
import com.manacommunity.api.payments.config.dto.CheckoutOptionsResponse;
import com.manacommunity.api.payments.config.dto.PlatformPaymentConfigResponse;
import com.manacommunity.api.payments.config.dto.UpdateCommunityPaymentSettingsRequest;
import com.manacommunity.api.payments.config.dto.UpdatePlatformPaymentConfigRequest;
import com.manacommunity.api.payments.config.entity.PlatformPaymentConfig;
import com.manacommunity.api.payments.config.service.PaymentConfigService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PaymentConfigController {

    private final PaymentConfigService paymentConfigService;

    /**
     * Returns the payment options a member sees at checkout for their community.
     * Any authenticated user may call this.
     */
    @GetMapping("/api/payments/checkout-options")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckoutOptionsResponse> getCheckoutOptions(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        if (communityId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(paymentConfigService.getCheckoutOptions(communityId));
    }

    // ─── Super admin — platform config ───────────────────────────────────────────

    @GetMapping("/api/admin/payment-config")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PlatformPaymentConfigResponse> getPlatformConfig() {
        return ResponseEntity.ok(PlatformPaymentConfigResponse.from(paymentConfigService.getPlatformConfig()));
    }

    @PutMapping("/api/admin/payment-config")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PlatformPaymentConfigResponse> updatePlatformConfig(
            @RequestBody UpdatePlatformPaymentConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(PlatformPaymentConfigResponse.from(
                paymentConfigService.updatePlatformConfig(req, principal.getId())));
    }

    // ─── Community admin — community payment settings ────────────────────────────

    @GetMapping("/api/community/payment-settings")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN','FINANCE_MGMT_ADMIN','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CommunitySettings> getCommunityPaymentSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        if (communityId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(paymentConfigService.getCommunityPaymentSettings(communityId));
    }

    @PutMapping("/api/community/payment-settings")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN','FINANCE_MGMT_ADMIN','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> updateCommunityPaymentSettings(
            @RequestBody UpdateCommunityPaymentSettingsRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        if (communityId == null) return ResponseEntity.badRequest().build();
        paymentConfigService.updateCommunityPaymentSettings(communityId, req);
        return ResponseEntity.ok().build();
    }
}
