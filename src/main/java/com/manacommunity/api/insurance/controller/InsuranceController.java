package com.manacommunity.api.insurance.controller;

import com.manacommunity.api.insurance.dto.InsuranceDtos.*;
import com.manacommunity.api.insurance.entity.InsuranceEnums.Category;
import com.manacommunity.api.insurance.service.*;
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

/**
 * End-user insurance API. All data access is scoped to the authenticated user (object-level authorisation
 * is enforced in the services). Disabled unless {@code insurance.enabled=true}.
 */
@RestController
@RequestMapping("/api/insurance")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "insurance.enabled", havingValue = "true")
public class InsuranceController {

    private final LoggedInUserService users;
    private final InsuranceCatalogueService catalogue;
    private final InsuranceConsentService consents;
    private final QuoteService quoteService;
    private final InsurancePolicyService policyService;
    private final ClaimsAssistanceService claims;
    private final GroupEnquiryService groupEnquiries;

    @GetMapping("/catalogue")
    public List<CatalogueItem> catalogue(@RequestParam Category category) {
        return catalogue.list(category);
    }

    // ---- consent
    @PostMapping("/consents")
    public ConsentView grant(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ConsentGrantRequest r) {
        return consents.grant(p.getId(), r);
    }

    @PostMapping("/consents/{id}/withdraw")
    public ConsentView withdraw(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return consents.withdraw(p.getId(), id);
    }

    @GetMapping("/consents")
    public List<ConsentView> consents(@AuthenticationPrincipal UserPrincipal p) {
        return consents.list(p.getId());
    }

    // ---- quotes
    @PostMapping("/quote-requests")
    public QuoteRequestView quote(@AuthenticationPrincipal UserPrincipal p,
                                  @RequestHeader(value = "Idempotency-Key", required = false) String key,
                                  @Valid @RequestBody QuoteSubmitRequest r) {
        AppUser user = users.resolve(p);
        return quoteService.submit(user, key, r);
    }

    @GetMapping("/quote-requests/{id}")
    public QuoteRequestView quote(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return quoteService.get(p.getId(), id);
    }

    // ---- policies
    @GetMapping("/policies")
    public List<PolicyView> policies(@AuthenticationPrincipal UserPrincipal p) {
        return policyService.list(p.getId());
    }

    @PostMapping("/policies")
    public PolicyView createPolicy(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody PolicyCreateRequest r) {
        return policyService.create(users.resolve(p), r);
    }

    @GetMapping("/policies/{id}")
    public PolicyView policy(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return policyService.get(p.getId(), id);
    }

    @DeleteMapping("/policies/{id}")
    public ResponseEntity<Void> deletePolicy(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        policyService.delete(p.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ---- claims assistance (Mana tickets, not insurer claims)
    @PostMapping("/claims-assistance")
    public ClaimCaseView createCase(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ClaimCaseRequest r) {
        return claims.create(users.resolve(p), r);
    }

    @GetMapping("/claims-assistance")
    public List<ClaimCaseView> cases(@AuthenticationPrincipal UserPrincipal p) {
        return claims.list(p.getId());
    }

    @GetMapping("/claims-assistance/{id}")
    public ClaimCaseView caseById(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return claims.get(p.getId(), id);
    }

    @PostMapping("/claims-assistance/{id}/insurer-reference")
    public ClaimCaseView insurerRef(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id,
                                    @Valid @RequestBody InsurerClaimRefRequest r) {
        return claims.attachInsurerReference(p.getId(), id, r.reference());
    }

    // ---- group enquiries
    @PostMapping("/group-enquiries")
    public GroupEnquiryView createEnquiry(@AuthenticationPrincipal UserPrincipal p,
                                          @Valid @RequestBody GroupEnquiryRequest r) {
        return groupEnquiries.create(users.resolve(p), r);
    }

    @GetMapping("/group-enquiries")
    public List<GroupEnquiryView> enquiries(@AuthenticationPrincipal UserPrincipal p) {
        return groupEnquiries.list(p.getId());
    }

    @GetMapping("/group-enquiries/{id}")
    public GroupEnquiryView enquiry(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return groupEnquiries.get(p.getId(), id);
    }
}
