package com.manacommunity.api.serviceplatform.interiors.controller;

import com.manacommunity.api.commerce.core.dto.CommerceCheckoutRequest;
import com.manacommunity.api.commerce.core.dto.CommerceOrderDto;
import com.manacommunity.api.payments.dto.PaymentOrderResponse;
import com.manacommunity.api.serviceplatform.interiors.dto.InteriorProjectDtos.*;
import com.manacommunity.api.serviceplatform.interiors.service.InteriorMarketplaceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-platform/interiors")
@RequiredArgsConstructor
public class InteriorMarketplaceController {
    private final InteriorMarketplaceService interiorsService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/designers")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<List<DesignerProfile>> designers(
            @RequestParam(required = false) String area,
            @RequestParam(required = false, name = "q") String query,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(interiorsService.listDesigners(communityId, area, query));
    }

    @GetMapping("/designers/{providerId}")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<DesignerProfile> designer(
            @PathVariable Long providerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(interiorsService.getDesigner(providerId, communityId));
    }

    @GetMapping("/designers/{providerId}/reviews")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<List<ReviewResponse>> designerReviews(
            @PathVariable Long providerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(interiorsService.getDesignerReviews(providerId, communityId));
    }

    @GetMapping("/designers/saved")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<List<DesignerProfile>> savedDesigners(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(interiorsService.listSavedDesigners(principal.getId(), communityId));
    }

    @PostMapping("/designers/{providerId}/save")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<Void> saveDesigner(@PathVariable Long providerId, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        interiorsService.saveDesigner(providerId, principal.getId(), communityId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/designers/{providerId}/save")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<Void> removeSavedDesigner(@PathVariable Long providerId, @AuthenticationPrincipal UserPrincipal principal) {
        interiorsService.removeSavedDesigner(providerId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inspirations")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<List<InspirationResponse>> inspirations(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(interiorsService.listInspirations(principal.getId()));
    }

    @PostMapping("/inspirations")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<InspirationResponse> saveInspiration(@Valid @RequestBody InspirationRequest request,
                                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interiorsService.saveInspiration(principal.getId(), request));
    }

    @DeleteMapping("/inspirations/{id}")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<Void> removeInspiration(@PathVariable Long id,@AuthenticationPrincipal UserPrincipal principal) {
        interiorsService.removeInspiration(id,principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/projects")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody BriefRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(interiorsService.createProject(request, user));
    }

    @PatchMapping("/projects/{projectId}/attachments")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<ProjectResponse> updateProjectAttachments(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectAttachmentsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(interiorsService.updateProjectAttachments(projectId, principal.getId(), communityId, request));
    }

    @GetMapping("/projects/my")
    @PreAuthorize("hasAnyAuthority('View Service Requests','Create Service Request')")
    public ResponseEntity<List<ProjectResponse>> myProjects(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(interiorsService.listMyProjects(principal.getId(), communityId));
    }

    @GetMapping("/projects/{projectId}")
    @PreAuthorize("hasAnyAuthority('View Service Requests','Create Service Request')")
    public ResponseEntity<ProjectResponse> project(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(interiorsService.getProject(projectId, principal.getId(), communityId));
    }

    @PostMapping("/projects/{projectId}/materials/checkout")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<CommerceOrderDto> checkoutProjectMaterials(
            @PathVariable Long projectId,
            @Valid @RequestBody CommerceCheckoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(interiorsService.checkoutProjectMaterials(projectId, user, communityId, request));
    }

    @PostMapping("/projects/{projectId}/milestones/{milestoneId}/approve")
    @PreAuthorize("hasAnyAuthority('View Service Requests','Create Service Request')")
    public ResponseEntity<ProjectResponse> approveMilestone(
            @PathVariable Long projectId,
            @PathVariable String milestoneId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(interiorsService.approveMilestone(projectId, milestoneId, principal.getId(), communityId));
    }

    @GetMapping("/providers/me/leads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectResponse>> designerLeads(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(interiorsService.listDesignerLeads(principal.getId()));
    }

    @PostMapping("/projects/{projectId}/quotes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuoteResponse> submitQuote(
            @PathVariable Long projectId,
            @Valid @RequestBody QuoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interiorsService.submitQuote(projectId, principal.getId(), request));
    }

    @PostMapping("/projects/{projectId}/quotes/{quoteId}/accept")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<ProjectResponse> acceptQuote(
            @PathVariable Long projectId,
            @PathVariable Long quoteId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(interiorsService.acceptQuote(projectId, quoteId, principal.getId(), communityId));
    }

    @PostMapping("/projects/{projectId}/review")
    @PreAuthorize("hasAnyAuthority('View Service Requests','Create Service Request')")
    public ResponseEntity<ReviewResponse> reviewProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interiorsService.submitProjectReview(projectId, principal.getId(), communityId, request));
    }

    @GetMapping("/projects/{projectId}/change-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChangeRequestResponse>> changeRequests(@PathVariable Long projectId,@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user=loggedInUserService.resolve(principal); Long communityId=user.getCommunity()!=null?user.getCommunity().getId():null;
        return ResponseEntity.ok(interiorsService.listChangeRequests(projectId,principal.getId(),communityId));
    }

    @PostMapping("/projects/{projectId}/change-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChangeRequestResponse> createChangeRequest(@PathVariable Long projectId,@Valid @RequestBody ChangeRequestCreate request,
                                                                      @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user=loggedInUserService.resolve(principal); Long communityId=user.getCommunity()!=null?user.getCommunity().getId():null;
        return ResponseEntity.status(HttpStatus.CREATED).body(interiorsService.createChangeRequest(projectId,principal.getId(),communityId,request));
    }

    @PostMapping("/projects/{projectId}/change-requests/{changeId}/decision")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChangeRequestResponse> decideChangeRequest(@PathVariable Long projectId,@PathVariable Long changeId,
            @Valid @RequestBody ChangeDecision request,@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user=loggedInUserService.resolve(principal); Long communityId=user.getCommunity()!=null?user.getCommunity().getId():null;
        return ResponseEntity.ok(interiorsService.decideChangeRequest(projectId,changeId,principal.getId(),communityId,request));
    }

    @GetMapping("/projects/{projectId}/payment-schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentMilestoneResponse>> paymentSchedule(@PathVariable Long projectId,@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user=loggedInUserService.resolve(principal); Long communityId=user.getCommunity()!=null?user.getCommunity().getId():null;
        return ResponseEntity.ok(interiorsService.listPaymentMilestones(projectId,principal.getId(),communityId));
    }

    @PostMapping("/projects/{projectId}/payment-schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentMilestoneResponse> addPaymentMilestone(@PathVariable Long projectId,@Valid @RequestBody PaymentMilestoneCreate request,
                                                                        @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user=loggedInUserService.resolve(principal); Long communityId=user.getCommunity()!=null?user.getCommunity().getId():null;
        return ResponseEntity.status(HttpStatus.CREATED).body(interiorsService.addPaymentMilestone(projectId,principal.getId(),communityId,request));
    }

    @PostMapping("/projects/{projectId}/payment-schedule/{milestoneId}/checkout")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<PaymentCheckoutResponse> checkoutMilestone(@PathVariable Long projectId,@PathVariable Long milestoneId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user=loggedInUserService.resolve(principal); Long communityId=user.getCommunity()!=null?user.getCommunity().getId():null;
        return ResponseEntity.status(HttpStatus.CREATED).body(interiorsService.createMilestoneCheckout(projectId,milestoneId,user,communityId));
    }

    @PostMapping("/projects/{projectId}/payment-schedule/{milestoneId}/authorize")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<PaymentMilestoneResponse> authorizeMilestone(@PathVariable Long projectId,@PathVariable Long milestoneId,
            @Valid @RequestBody MilestonePaymentAuthorization request,@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user=loggedInUserService.resolve(principal); Long communityId=user.getCommunity()!=null?user.getCommunity().getId():null;
        return ResponseEntity.ok(interiorsService.authorizeMilestone(projectId,milestoneId,request.gateway(),request.orderId(),request.paymentId(),principal.getId(),communityId));
    }

    @PostMapping("/projects/{projectId}/payment-schedule/{milestoneId}/release")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<PaymentMilestoneResponse> releaseMilestone(@PathVariable Long projectId,@PathVariable Long milestoneId,
            @RequestBody(required=false) PaymentRelease request,@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user=loggedInUserService.resolve(principal); Long communityId=user.getCommunity()!=null?user.getCommunity().getId():null;
        return ResponseEntity.ok(interiorsService.releaseMilestone(projectId,milestoneId,principal.getId(),communityId,request));
    }

    @PutMapping("/projects/{projectId}/payment-schedule/{milestoneId}/invoice")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentMilestoneResponse> linkInvoice(@PathVariable Long projectId,@PathVariable Long milestoneId,
            @Valid @RequestBody PaymentInvoiceLink request,@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user=loggedInUserService.resolve(principal); Long communityId=user.getCommunity()!=null?user.getCommunity().getId():null;
        return ResponseEntity.ok(interiorsService.linkMilestoneInvoice(projectId,milestoneId,principal.getId(),communityId,request));
    }

    @PostMapping("/providers/me/credentials")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CredentialResponse> submitCredential(@Valid @RequestBody CredentialCreate request,@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interiorsService.submitCredential(principal.getId(),request));
    }

    @GetMapping("/providers/me/credentials")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CredentialResponse>> myCredentials(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(interiorsService.listMyCredentials(principal.getId()));
    }

    @GetMapping("/providers/me/credentials/{credentialId}/audit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CredentialAuditResponse>> credentialAudit(@PathVariable Long credentialId,@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(interiorsService.credentialAudit(credentialId,principal.getId()));
    }

    @GetMapping("/provider-credentials/pending")
    @PreAuthorize("hasAuthority('Manage Service Providers')")
    public ResponseEntity<List<CredentialResponse>> pendingCredentials() {
        return ResponseEntity.ok(interiorsService.listPendingCredentials());
    }

    @PostMapping("/provider-credentials/{credentialId}/review")
    @PreAuthorize("hasAuthority('Manage Service Providers')")
    public ResponseEntity<CredentialResponse> reviewCredential(@PathVariable Long credentialId,@Valid @RequestBody CredentialReview request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(interiorsService.reviewCredential(credentialId,principal.getId(),request));
    }

    @GetMapping("/provider-credentials/{credentialId}/audit")
    @PreAuthorize("hasAuthority('Manage Service Providers')")
    public ResponseEntity<List<CredentialAuditResponse>> credentialReviewAudit(@PathVariable Long credentialId) {
        return ResponseEntity.ok(interiorsService.credentialAuditForReview(credentialId));
    }

    @PostMapping("/projects/{projectId}/materials/checkout-boq")
    @PreAuthorize("hasAuthority('Create Service Request')")
    public ResponseEntity<CommerceOrderDto> checkoutProjectBoq(@PathVariable Long projectId,@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user=loggedInUserService.resolve(principal); Long communityId=user.getCommunity()!=null?user.getCommunity().getId():null;
        return ResponseEntity.status(HttpStatus.CREATED).body(interiorsService.checkoutProjectBoq(projectId,user,communityId));
    }
}
