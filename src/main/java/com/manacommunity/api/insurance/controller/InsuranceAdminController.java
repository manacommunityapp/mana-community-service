package com.manacommunity.api.insurance.controller;

import com.manacommunity.api.insurance.dto.InsuranceDtos.*;
import com.manacommunity.api.insurance.service.ClaimsAssistanceService;
import com.manacommunity.api.insurance.service.InsuranceAdminService;
import com.manacommunity.api.insurance.service.InsuranceAdminService.ProductVersionSubmitRequest;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Admin/partner operations. Role is re-checked server-side in the services (SUPER_ADMIN / ADMIN). */
@RestController
@RequestMapping("/api/insurance/admin")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "insurance.enabled", havingValue = "true")
public class InsuranceAdminController {

    private final LoggedInUserService users;
    private final InsuranceAdminService admin;
    private final ClaimsAssistanceService claims;

    private AppUser me(UserPrincipal p) { return users.resolve(p); }

    @GetMapping("/partners")
    public List<PartnerView> partners(@AuthenticationPrincipal UserPrincipal p) {
        return admin.listPartners(me(p));
    }

    @PostMapping("/partners")
    public PartnerView create(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody PartnerCreateRequest r) {
        return admin.createPartner(me(p), r);
    }

    @PostMapping("/partners/{id}/approve")
    public PartnerView approve(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id,
                               @Valid @RequestBody PartnerApproveRequest r) {
        return admin.approvePartner(me(p), id, r);
    }

    @PostMapping("/partners/{id}/suspend")
    public PartnerView suspend(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return admin.suspendPartner(me(p), id);
    }

    @PostMapping("/product-versions")
    public Map<String, Long> submitVersion(@AuthenticationPrincipal UserPrincipal p,
                                           @Valid @RequestBody ProductVersionSubmitRequest r) {
        return Map.of("productVersionId", admin.submitProductVersion(me(p), r));
    }

    @PostMapping("/product-versions/{id}/approve")
    public ResponseEntity<Void> approveVersion(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        admin.approveProductVersion(me(p), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/webhook-events/failed")
    public List<WebhookEventView> failedWebhooks(@AuthenticationPrincipal UserPrincipal p) {
        return admin.failedWebhooks(me(p));
    }

    @PatchMapping("/claims-assistance/{id}")
    public ClaimCaseView updateCase(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id,
                                    @Valid @RequestBody CaseUpdateRequest r) {
        return claims.adminUpdate(me(p), id, r);
    }
}
