package com.manacommunity.api.serviceplatform.interiors.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.commerce.core.dto.CommerceCheckoutRequest;
import com.manacommunity.api.commerce.core.dto.CommerceOrderDto;
import com.manacommunity.api.commerce.core.dto.CommerceOrderItemDto;
import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.service.CommerceCoreService;
import com.manacommunity.api.commerce.core.model.CommerceProduct;
import com.manacommunity.api.commerce.core.repository.CommerceProductRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.media.entity.MediaModule;
import com.manacommunity.api.media.entity.MediaObject;
import com.manacommunity.api.media.repository.MediaRepository;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.serviceplatform.dto.request.AssignProviderRequest;
import com.manacommunity.api.serviceplatform.dto.request.CreateServiceRequestDto;
import com.manacommunity.api.serviceplatform.dto.response.ServiceOfferingResponse;
import com.manacommunity.api.serviceplatform.dto.response.ServiceProviderResponse;
import com.manacommunity.api.serviceplatform.dto.response.WorkOrderResponse;
import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import com.manacommunity.api.serviceplatform.interiors.dto.InteriorProjectDtos.*;
import com.manacommunity.api.serviceplatform.interiors.entity.InteriorProjectInvitation;
import com.manacommunity.api.serviceplatform.interiors.entity.InteriorSavedDesigner;
import com.manacommunity.api.serviceplatform.interiors.entity.InteriorSavedInspiration;
import com.manacommunity.api.serviceplatform.interiors.entity.InteriorChangeRequest;
import com.manacommunity.api.serviceplatform.interiors.entity.InteriorPaymentMilestone;
import com.manacommunity.api.serviceplatform.interiors.entity.InteriorPaymentEvent;
import com.manacommunity.api.serviceplatform.interiors.entity.ServiceProviderCredential;
import com.manacommunity.api.serviceplatform.interiors.entity.ServiceProviderCredentialAudit;
import com.manacommunity.api.serviceplatform.interiors.entity.InteriorQuoteLineItem;
import com.manacommunity.api.serviceplatform.interiors.repository.InteriorProjectInvitationRepository;
import com.manacommunity.api.serviceplatform.interiors.repository.InteriorSavedDesignerRepository;
import com.manacommunity.api.serviceplatform.interiors.repository.InteriorSavedInspirationRepository;
import com.manacommunity.api.serviceplatform.interiors.repository.InteriorChangeRequestRepository;
import com.manacommunity.api.serviceplatform.interiors.repository.InteriorPaymentMilestoneRepository;
import com.manacommunity.api.serviceplatform.interiors.repository.InteriorPaymentEventRepository;
import com.manacommunity.api.serviceplatform.interiors.repository.ServiceProviderCredentialRepository;
import com.manacommunity.api.serviceplatform.interiors.repository.ServiceProviderCredentialAuditRepository;
import com.manacommunity.api.serviceplatform.interiors.repository.InteriorQuoteLineItemRepository;
import com.manacommunity.api.serviceplatform.pricing.entity.ServiceQuote;
import com.manacommunity.api.serviceplatform.pricing.repository.ServiceQuoteRepository;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceRequestRepository;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import com.manacommunity.api.serviceplatform.review.dto.CreateReviewRequest;
import com.manacommunity.api.serviceplatform.review.repository.ServiceReviewRepository;
import com.manacommunity.api.serviceplatform.review.service.ReviewService;
import com.manacommunity.api.serviceplatform.service.ServiceProviderService;
import com.manacommunity.api.serviceplatform.service.ServiceRequestService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.payments.dto.CreateOrderRequest;
import com.manacommunity.api.payments.dto.PaymentOrderResponse;
import com.manacommunity.api.payments.entity.RazorpayOrder;
import com.manacommunity.api.payments.repository.RazorpayOrderRepository;
import com.manacommunity.api.payments.service.RazorpayPaymentService;
import com.manacommunity.api.payments.dto.CashfreeCreateOrderRequest;
import com.manacommunity.api.payments.dto.CashfreeOrderResponse;
import com.manacommunity.api.payments.entity.CashfreeOrder;
import com.manacommunity.api.payments.repository.CashfreeOrderRepository;
import com.manacommunity.api.payments.service.CashfreePaymentService;
import org.springframework.beans.factory.ObjectProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InteriorMarketplaceService {
    private static final String INTERIOR_CATEGORY = "interior-works";
    private static final String RENOVATION_CATEGORY = "renovation";
    private static final int MAX_MATCHED_DESIGNERS = 5;

    private final ObjectMapper objectMapper;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceProviderRepository providerRepository;
    private final ProviderServiceOfferingRepository offeringRepository;
    private final ServiceRequestRepository requestRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ServiceQuoteRepository quoteRepository;
    private final InteriorQuoteLineItemRepository quoteLineItemRepository;
    private final ServiceReviewRepository reviewRepository;
    private final InteriorProjectInvitationRepository invitationRepository;
    private final InteriorSavedDesignerRepository savedDesignerRepository;
    private final InteriorSavedInspirationRepository inspirationRepository;
    private final InteriorChangeRequestRepository changeRequestRepository;
    private final InteriorPaymentMilestoneRepository paymentMilestoneRepository;
    private final InteriorPaymentEventRepository paymentEventRepository;
    private final ServiceProviderCredentialRepository credentialRepository;
    private final ServiceProviderCredentialAuditRepository credentialAuditRepository;
    private final CommerceProductRepository productRepository;
    private final RazorpayOrderRepository razorpayOrderRepository;
    private final ObjectProvider<RazorpayPaymentService> razorpayPaymentService;
    private final CashfreeOrderRepository cashfreeOrderRepository;
    private final ObjectProvider<CashfreePaymentService> cashfreePaymentService;
    private final MediaRepository mediaRepository;
    private final CommerceCoreService commerceCoreService;
    private final ServiceProviderService providerService;
    private final ServiceRequestService requestService;
    private final ReviewService reviewService;

    @Transactional(readOnly = true)
    public List<DesignerProfile> listDesigners(Long communityId, String area, String query) {
        List<ServiceCategory> categories = interiorCategories();
        if (categories.isEmpty()) return List.of();
        Set<Long> categoryIds = categories.stream().map(ServiceCategory::getId).collect(Collectors.toSet());
        return providerRepository.findVerifiedInteriorProviders(communityId, categoryIds).stream()
                .map(provider -> providerService.getProviderForCommunity(provider.getId(), communityId))
                .filter(profile -> area == null || area.isBlank() || containsIgnoreCase(profile.getServiceAreas(), area))
                .filter(profile -> query == null || query.isBlank() || profileMatches(profile, query))
                .map(this::toDesignerProfile)
                .sorted(Comparator.comparing(DesignerProfile::avgRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DesignerProfile getDesigner(Long providerId, Long communityId) {
        ServiceProviderResponse profile = providerService.getProviderForCommunity(providerId, communityId);
        if (profile.getOfferings() == null || profile.getOfferings().stream().noneMatch(this::isInteriorOffering)) {
            throw new ResourceNotFoundException("InteriorDesigner", providerId);
        }
        return toDesignerProfile(profile);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getDesignerReviews(Long providerId, Long communityId) {
        getDesigner(providerId, communityId);
        return reviewService.getReviewsByProvider(providerId).stream().map(review -> new ReviewResponse(
                review.getId(), review.getWorkOrderId(), review.getProviderId(), review.getProviderName(),
                review.getReviewerId(), review.getReviewerName(), review.getRating(), review.getQualityRating(),
                review.getPunctualityRating(), review.getBehaviorRating(), review.getComment(),
                review.getProviderResponse(), review.isVerifiedResident(), review.getCreatedAt())).toList();
    }

    @Transactional
    public ReviewResponse submitProjectReview(Long requestId, Long userId, Long communityId, ReviewRequest request) {
        ownedProject(requestId, userId, communityId);
        WorkOrder workOrder = workOrderRepository.findByServiceRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("A review is available after the project is assigned"));
        if (workOrder.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new IllegalStateException("You can review this project after the work is completed");
        }
        if (reviewRepository.findByWorkOrderId(workOrder.getId()).isPresent()) {
            throw new IllegalStateException("This project has already been reviewed");
        }
        CreateReviewRequest review = new CreateReviewRequest();
        review.setWorkOrderId(workOrder.getId());
        review.setReviewerId(userId);
        review.setRating(request.rating());
        review.setQualityRating(request.qualityRating());
        review.setPunctualityRating(request.punctualityRating());
        review.setBehaviorRating(request.behaviorRating());
        review.setComment(request.comment());
        var result = reviewService.createReview(review);
        return new ReviewResponse(result.getId(), result.getWorkOrderId(), result.getProviderId(), result.getProviderName(),
                result.getReviewerId(), result.getReviewerName(), result.getRating(), result.getQualityRating(),
                result.getPunctualityRating(), result.getBehaviorRating(), result.getComment(),
                result.getProviderResponse(), result.isVerifiedResident(), result.getCreatedAt());
    }

    @Transactional
    public ProjectResponse createProject(BriefRequest brief, AppUser user) {
        if (user.getCommunity() == null) throw new IllegalStateException("A community is required to create an interiors project");
        ServiceCategory category = categoryRepository.findBySlugAndActiveTrue(INTERIOR_CATEGORY)
                .orElseThrow(() -> new ResourceNotFoundException("InteriorWorksCategory", INTERIOR_CATEGORY));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("kind", "INTERIOR_DESIGN");
        details.put("propertyType", brief.propertyType());
        details.put("homeSize", brief.homeSize());
        details.put("locality", brief.locality());
        details.put("stylePreference", brief.stylePreference());
        details.put("focusAreas", brief.focusAreas() == null ? List.of() : brief.focusAreas());
        details.put("budgetMin", brief.budgetMin());
        details.put("budgetMax", brief.budgetMax());
        details.put("projectScope", brief.projectScope());
        details.put("preferredStart", brief.preferredStart());

        CreateServiceRequestDto requestDto = new CreateServiceRequestDto();
        requestDto.setCategoryId(category.getId());
        requestDto.setTitle(brief.homeSize() + " " + brief.propertyType() + " interiors · " + brief.locality());
        requestDto.setDescription(brief.projectScope());
        requestDto.setPreferredDate(brief.preferredStart());
        requestDto.setAddress(brief.locality());
        requestDto.setUrgency("NORMAL");
        requestDto.setCustomFieldValues(writeJson(details));
        requestDto.setSubmitImmediately(true);

        var created = requestService.createRequest(requestDto, user);
        ServiceRequest request = requestRepository.findById(created.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", created.getId()));

        List<DesignerProfile> candidateProfiles = listDesigners(user.getCommunity().getId(), brief.locality(), null);
        if (candidateProfiles.isEmpty()) candidateProfiles = listDesigners(user.getCommunity().getId(), null, null);
        Set<Long> requestedIds = brief.designerIds() == null ? Set.of() : new LinkedHashSet<>(brief.designerIds());
        List<DesignerProfile> selected = requestedIds.isEmpty()
                ? candidateProfiles.stream().limit(MAX_MATCHED_DESIGNERS).toList()
                : candidateProfiles.stream().filter(profile -> requestedIds.contains(profile.id())).toList();
        if (!requestedIds.isEmpty() && selected.size() != requestedIds.size()) {
            throw new IllegalArgumentException("One or more selected designers are not available for this community");
        }
        selected.forEach(profile -> invitationRepository.save(InteriorProjectInvitation.builder()
                .serviceRequest(request)
                .provider(providerRepository.findById(profile.id()).orElseThrow())
                .status("INVITED")
                .build()));
        return toProject(request, null);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listMyProjects(Long userId, Long communityId) {
        return requestRepository.findByRequesterIdAndCategorySlugOrderByCreatedAtDesc(userId, INTERIOR_CATEGORY).stream()
                .filter(request -> request.getCommunity() != null && Objects.equals(request.getCommunity().getId(), communityId))
                .map(request -> toProject(request, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long requestId, Long userId, Long communityId) {
        ServiceRequest request = ownedProject(requestId, userId, communityId);
        return toProject(request, null);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listDesignerLeads(Long userId) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));
        return invitationRepository.findByProviderIdAndStatusInOrderByCreatedAtDesc(provider.getId(), List.of("INVITED", "QUOTED")).stream()
                .map(InteriorProjectInvitation::getServiceRequest)
                .filter(request -> request.getStatus() == ServiceRequestStatus.SUBMITTED || request.getStatus() == ServiceRequestStatus.MATCHING)
                .map(request -> toProject(request, provider.getId()))
                .toList();
    }

    @Transactional
    public QuoteResponse submitQuote(Long requestId, Long userId, QuoteRequest quoteRequest) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", userId));
        if (!"VERIFIED".equals(provider.getVerificationStatus().name())) {
            throw new UnauthorizedActionException("Only verified designers can submit project quotes");
        }
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("InteriorProject", requestId));
        if (!"interior-works".equals(request.getCategory().getSlug())) {
            throw new ResourceNotFoundException("InteriorProject", requestId);
        }
        if (request.getStatus() != ServiceRequestStatus.SUBMITTED && request.getStatus() != ServiceRequestStatus.MATCHING) {
            throw new IllegalStateException("This project is no longer accepting quotes");
        }
        InteriorProjectInvitation invitation = invitationRepository.findByServiceRequestIdAndProviderId(requestId, provider.getId())
                .orElseThrow(() -> new UnauthorizedActionException("You are not invited to quote this project"));
        if ("DECLINED".equals(invitation.getStatus())) throw new IllegalStateException("You declined this project invitation");

        List<InteriorQuoteLineItem> lineItems = buildLineItems(quoteRequest.lineItems());
        BigDecimal calculatedTotal = lineItems.stream().map(InteriorQuoteLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        boolean hasLineItems = !lineItems.isEmpty();
        if (hasLineItems && quoteRequest.quotedAmount().subtract(calculatedTotal).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("The quotation total must equal the sum of its BOQ line items");
        }
        BigDecimal materialTotal = lineItems.stream().filter(item -> item.getCategory().toUpperCase(Locale.ROOT).contains("MATERIAL"))
                .map(item -> item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal laborTotal = lineItems.stream().filter(item -> {
                    String category = item.getCategory().toUpperCase(Locale.ROOT);
                    return category.contains("LABOUR") || category.contains("LABOR");
                }).map(item -> item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal calculatedTax = lineItems.stream().map(item -> {
                    BigDecimal base = item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal margin = base.multiply(item.getMarginPercent()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    BigDecimal taxable = base.add(margin);
                    return item.getLineTotal().subtract(taxable);
                }).reduce(BigDecimal.ZERO, BigDecimal::add);

        ServiceQuote quote = quoteRepository.findByServiceRequestIdAndProviderId(requestId, provider.getId())
                .orElseGet(() -> ServiceQuote.builder().serviceRequest(request).provider(provider).status("SUBMITTED").build());
        quote.setQuotedAmount(hasLineItems ? calculatedTotal : quoteRequest.quotedAmount());
        quote.setEstimatedDurationDays(quoteRequest.estimatedDurationDays());
        quote.setEstimatedDurationMinutes(Math.multiplyExact(quoteRequest.estimatedDurationDays(), 1440));
        quote.setMaterialAllowance(hasLineItems ? materialTotal : zeroIfNull(quoteRequest.materialAllowance()));
        quote.setLaborAmount(hasLineItems ? laborTotal : zeroIfNull(quoteRequest.laborAmount()));
        quote.setTaxAmount(hasLineItems ? calculatedTax : zeroIfNull(quoteRequest.taxAmount()));
        quote.setNotes(quoteRequest.notes());
        quote.setStatus("SUBMITTED");
        quote = quoteRepository.save(quote);
        quoteLineItemRepository.deleteByQuoteId(quote.getId());
        if (hasLineItems) {
            ServiceQuote savedQuote = quote;
            lineItems.forEach(item -> item.setQuote(savedQuote));
            quoteLineItemRepository.saveAll(lineItems);
        }
        invitation.setStatus("QUOTED");
        invitationRepository.save(invitation);
        return toQuote(quote);
    }

    @Transactional
    public ProjectResponse acceptQuote(Long requestId, Long quoteId, Long userId, Long communityId) {
        ServiceRequest request = ownedProject(requestId, userId, communityId);
        ServiceQuote accepted = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("InteriorQuote", quoteId));
        if (!Objects.equals(accepted.getServiceRequest().getId(), requestId) || !"SUBMITTED".equals(accepted.getStatus())) {
            throw new IllegalArgumentException("This quote is not available for this project");
        }
        AssignProviderRequest assignment = new AssignProviderRequest();
        assignment.setProviderId(accepted.getProvider().getId());
        requestService.assignProvider(requestId, assignment);

        request = requestRepository.findById(requestId).orElseThrow();
        request.setEstimatedCost(accepted.getQuotedAmount());
        requestRepository.save(request);
        List<ServiceQuote> quotes = quoteRepository.findByServiceRequestIdOrderByCreatedAtDesc(requestId);
        quotes.forEach(quote -> quote.setStatus(quote.getId().equals(quoteId) ? "ACCEPTED" : "REJECTED"));
        quoteRepository.saveAll(quotes);
        invitationRepository.findByServiceRequestId(requestId).forEach(invitation -> {
            invitation.setStatus(invitation.getProvider().getId().equals(accepted.getProvider().getId()) ? "ACCEPTED" : "CLOSED");
        });
        invitationRepository.flush();
        return toProject(request, null);
    }

    @Transactional(readOnly = true)
    public List<DesignerProfile> listSavedDesigners(Long userId, Long communityId) {
        return savedDesignerRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(saved -> getDesigner(saved.getProviderId(), communityId)).toList();
    }

    @Transactional
    public void saveDesigner(Long providerId, Long userId, Long communityId) {
        getDesigner(providerId, communityId);
        if (!savedDesignerRepository.existsByUserIdAndProviderId(userId, providerId)) {
            savedDesignerRepository.save(InteriorSavedDesigner.builder().userId(userId).providerId(providerId).build());
        }
    }

    @Transactional
    public void removeSavedDesigner(Long providerId, Long userId) {
        savedDesignerRepository.deleteByUserIdAndProviderId(userId, providerId);
    }

    @Transactional(readOnly = true)
    public List<InspirationResponse> listInspirations(Long userId) {
        return inspirationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toInspiration).toList();
    }

    @Transactional
    public InspirationResponse saveInspiration(Long userId, InspirationRequest request) {
        ServiceRequest project = null;
        if (request.projectId()!=null) {
            project=requestRepository.findById(request.projectId()).orElseThrow(() -> new ResourceNotFoundException("InteriorProject",request.projectId()));
            if (!"interior-works".equals(project.getCategory().getSlug()) || !Objects.equals(project.getRequester().getId(),userId)) {
                throw new UnauthorizedActionException("You can only link inspiration to your own interiors project");
            }
        }
        String sourceUrl=request.sourceUrl().trim();
        InteriorSavedInspiration saved = inspirationRepository.findByUserIdAndSourceUrl(userId, sourceUrl)
                .orElseGet(() -> InteriorSavedInspiration.builder().userId(userId).sourceUrl(sourceUrl).build());
        saved.setServiceRequest(project);
        saved.setImageUrl(request.imageUrl());
        saved.setTitle(request.title());
        saved.setNotes(request.notes());
        saved.setTags(request.tags());
        return toInspiration(inspirationRepository.save(saved));
    }

    @Transactional
    public void removeInspiration(Long id, Long userId) {
        InteriorSavedInspiration saved = inspirationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SavedInspiration", id));
        inspirationRepository.delete(saved);
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestResponse> listChangeRequests(Long requestId, Long userId, Long communityId) {
        memberProject(requestId, userId, communityId);
        return changeRequestRepository.findByServiceRequestIdOrderByCreatedAtDesc(requestId).stream().map(this::toChangeRequest).toList();
    }

    @Transactional
    public ChangeRequestResponse createChangeRequest(Long requestId, Long userId, Long communityId, ChangeRequestCreate input) {
        ServiceRequest project = memberProject(requestId, userId, communityId);
        if (project.getAssignedProvider() == null) throw new IllegalStateException("A designer must be assigned before requesting scope changes");
        if (input.proposedStartDate() != null && input.proposedEndDate() != null && input.proposedEndDate().isBefore(input.proposedStartDate())) {
            throw new IllegalArgumentException("Proposed end date must be on or after the proposed start date");
        }
        InteriorChangeRequest request = InteriorChangeRequest.builder().serviceRequest(project).proposedBy(userId)
                .description(input.description().trim()).scopeDelta(input.scopeDelta()).priceDelta(input.priceDelta())
                .proposedStartDate(input.proposedStartDate()).proposedEndDate(input.proposedEndDate()).build();
        return toChangeRequest(changeRequestRepository.save(request));
    }

    @Transactional
    public ChangeRequestResponse decideChangeRequest(Long requestId, Long changeId, Long userId, Long communityId, ChangeDecision decision) {
        ServiceRequest project = memberProject(requestId, userId, communityId);
        InteriorChangeRequest request = changeRequestRepository.lockByIdAndServiceRequestId(changeId, requestId)
                .orElseThrow(() -> new ResourceNotFoundException("InteriorChangeRequest", changeId));
        if (!"PENDING".equals(request.getStatus())) throw new IllegalStateException("This change request has already been decided");
        if (Objects.equals(request.getProposedBy(), userId)) throw new UnauthorizedActionException("A requester cannot decide their own change request");
        String next = decision.decision().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVED", "REJECTED").contains(next)) throw new IllegalArgumentException("Decision must be APPROVED or REJECTED");
        request.setStatus(next);
        request.setDecisionNote(decision.note());
        request.setDecidedBy(userId);
        request.setDecidedAt(java.time.LocalDateTime.now());
        if ("APPROVED".equals(next)) {
            if (request.getPriceDelta() != null) {
                BigDecimal updated = zeroIfNull(project.getEstimatedCost()).add(request.getPriceDelta());
                if (updated.signum() < 0) throw new IllegalArgumentException("A change cannot reduce the project price below zero");
                project.setEstimatedCost(updated);
            }
            if (request.getProposedStartDate() != null) project.setPreferredDate(request.getProposedStartDate());
            if (request.getProposedEndDate() != null && project.getAssignedProvider() != null) {
                workOrderRepository.findByServiceRequestId(requestId).ifPresent(order -> {
                    if (order.getScheduledEnd() != null && order.getScheduledEnd().toLocalDate().isBefore(request.getProposedEndDate())) {
                        order.setScheduledEnd(java.time.LocalDateTime.of(request.getProposedEndDate(),order.getScheduledEnd().toLocalTime()));
                        workOrderRepository.save(order);
                    }
                });
            }
            Map<String,Object> details = new LinkedHashMap<>(readDetails(project.getCustomFieldValues()));
            if (request.getScopeDelta() != null && !request.getScopeDelta().isBlank()) {
                String existing = stringValue(details,"projectScope");
                details.put("projectScope", existing.isBlank() ? request.getScopeDelta() : existing + "\n\nApproved change: " + request.getScopeDelta());
                project.setDescription(String.valueOf(details.get("projectScope")));
            }
            project.setCustomFieldValues(writeJson(details));
            requestRepository.save(project);
        }
        return toChangeRequest(changeRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<PaymentMilestoneResponse> listPaymentMilestones(Long requestId, Long userId, Long communityId) {
        memberProject(requestId,userId,communityId);
        return paymentMilestoneRepository.findByServiceRequestIdOrderByDueDateAscIdAsc(requestId).stream().map(this::toPaymentMilestone).toList();
    }

    @Transactional
    public PaymentMilestoneResponse addPaymentMilestone(Long requestId, Long userId, Long communityId, PaymentMilestoneCreate input) {
        ServiceRequest project = memberProject(requestId,userId,communityId);
        ServiceProvider provider = project.getAssignedProvider();
        if (provider == null) throw new IllegalStateException("A designer must be assigned before creating a payment schedule");
        BigDecimal scheduled = paymentMilestoneRepository.findByServiceRequestIdOrderByDueDateAscIdAsc(requestId).stream()
                .map(InteriorPaymentMilestone::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        if (project.getEstimatedCost() != null && scheduled.add(input.amount()).compareTo(project.getEstimatedCost()) > 0) {
            throw new IllegalArgumentException("Payment milestones cannot exceed the accepted project quote");
        }
        InteriorPaymentMilestone milestone = InteriorPaymentMilestone.builder().serviceRequest(project).provider(provider)
                .title(input.title().trim()).description(input.description()).amount(input.amount()).dueDate(input.dueDate())
                .invoiceReference(input.invoiceReference()).invoiceUrl(input.invoiceUrl()).build();
        return toPaymentMilestone(paymentMilestoneRepository.save(milestone));
    }

    @Transactional
    public PaymentCheckoutResponse createMilestoneCheckout(Long requestId, Long milestoneId, AppUser user, Long communityId) {
        ownedProject(requestId,user.getId(),communityId);
        InteriorPaymentMilestone milestone = paymentMilestoneRepository.lockByIdAndServiceRequestId(milestoneId,requestId)
                .orElseThrow(() -> new ResourceNotFoundException("InteriorPaymentMilestone",milestoneId));
        if (!Set.of("SCHEDULED","PAYMENT_PENDING").contains(milestone.getStatus())) throw new IllegalStateException("This payment milestone cannot be paid in its current state");
        String description="Interior project milestone: " + milestone.getTitle();
        CashfreePaymentService cashfree=cashfreePaymentService.getIfAvailable();
        if(cashfree!=null){
            CashfreeCreateOrderRequest request=new CashfreeCreateOrderRequest();
            request.setAmount(milestone.getAmount()); request.setReferenceType("INTERIORS_MILESTONE");
            request.setReferenceId(milestone.getId()); request.setDescription(description);
            CashfreeOrderResponse order=cashfree.createOrder(request,user,user.getCommunity());
            milestone.setPaymentGateway("CASHFREE"); milestone.setCashfreeManaOrderId(order.getManaOrderId());
            milestone.setStatus("PAYMENT_PENDING"); paymentMilestoneRepository.save(milestone);
            return new PaymentCheckoutResponse("CASHFREE",order.getManaOrderId(),order.getAmount(),order.getCurrency(),
                    order.getPaymentSessionId(),order.getAppId(),null);
        }
        RazorpayPaymentService razorpay=razorpayPaymentService.getIfAvailable();
        if(razorpay==null) throw new IllegalStateException("Online payments are not enabled");
        CreateOrderRequest request=new CreateOrderRequest();
        request.setAmount(milestone.getAmount()); request.setReferenceType("INTERIORS_MILESTONE");
        request.setReferenceId(milestone.getId()); request.setDescription(description);
        PaymentOrderResponse order=razorpay.createOrder(request,user,user.getCommunity());
        milestone.setPaymentGateway("RAZORPAY"); milestone.setRazorpayOrderId(order.getRazorpayOrderId());
        milestone.setStatus("PAYMENT_PENDING"); paymentMilestoneRepository.save(milestone);
        return new PaymentCheckoutResponse("RAZORPAY",order.getRazorpayOrderId(),order.getAmount(),order.getCurrency(),
                null,null,order.getKeyId());
    }

    @Transactional
    public PaymentMilestoneResponse authorizeMilestone(Long requestId, Long milestoneId, String gatewayName, String orderId,
            String paymentId, Long userId, Long communityId) {
        ownedProject(requestId,userId,communityId);
        InteriorPaymentMilestone milestone = paymentMilestoneRepository.lockByIdAndServiceRequestId(milestoneId,requestId)
                .orElseThrow(() -> new ResourceNotFoundException("InteriorPaymentMilestone",milestoneId));
        String gateway=gatewayName.toUpperCase(Locale.ROOT);
        String expectedOrder="CASHFREE".equals(gateway)?milestone.getCashfreeManaOrderId():milestone.getRazorpayOrderId();
        if (!"PAYMENT_PENDING".equals(milestone.getStatus()) || !Objects.equals(milestone.getPaymentGateway(),gateway) || !Objects.equals(expectedOrder,orderId)) {
            throw new IllegalStateException("The milestone has no matching payment awaiting verification");
        }
        if("CASHFREE".equals(gateway)){
            CashfreePaymentService cashfree=cashfreePaymentService.getIfAvailable();
            if(cashfree==null) throw new IllegalStateException("Cashfree payments are not enabled");
            cashfree.verifyPayment(orderId);
            CashfreeOrder paid=cashfreeOrderRepository.findByManaOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder",orderId));
            if(!validMilestonePayment(paid.getStatus(),paid.getUser().getId(),paid.getReferenceType(),paid.getReferenceId(),paid.getAmount(),userId,milestoneId,milestone.getAmount()))
                throw new UnauthorizedActionException("The gateway payment does not match this project milestone");
        }else if("RAZORPAY".equals(gateway)){
            RazorpayOrder paid = razorpayOrderRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder",orderId));
            if (!validMilestonePayment(paid.getStatus(),paid.getUser().getId(),paid.getReferenceType(),paid.getReferenceId(),paid.getAmount(),userId,milestoneId,milestone.getAmount())
                    || !Objects.equals(paid.getRazorpayPaymentId(),paymentId)) {
                throw new UnauthorizedActionException("The gateway payment does not match this project milestone");
            }
            milestone.setRazorpayPaymentId(paymentId);
        }else{
            throw new IllegalArgumentException("Unsupported payment gateway");
        }
        milestone.setAuthorizedAt(java.time.LocalDateTime.now());
        milestone.setStatus("AUTHORIZED");
        savePaymentEvent(milestone,userId,"AUTHORIZED",gateway+":"+orderId);
        return toPaymentMilestone(paymentMilestoneRepository.save(milestone));
    }

    @Transactional
    public PaymentMilestoneResponse releaseMilestone(Long requestId, Long milestoneId, Long userId, Long communityId, PaymentRelease release) {
        ownedProject(requestId,userId,communityId);
        InteriorPaymentMilestone milestone = paymentMilestoneRepository.lockByIdAndServiceRequestId(milestoneId,requestId)
                .orElseThrow(() -> new ResourceNotFoundException("InteriorPaymentMilestone",milestoneId));
        if (!"AUTHORIZED".equals(milestone.getStatus())) throw new IllegalStateException("Only an authorized milestone payment can be released");
        milestone.setStatus("RELEASED");
        milestone.setReleasedAt(java.time.LocalDateTime.now());
        milestone.setPayoutReference(release == null ? null : release.payoutReference());
        savePaymentEvent(milestone,userId,"RELEASED",milestone.getPayoutReference());
        return toPaymentMilestone(paymentMilestoneRepository.save(milestone));
    }

    @Transactional
    public PaymentMilestoneResponse linkMilestoneInvoice(Long requestId, Long milestoneId, Long userId, Long communityId, PaymentInvoiceLink input) {
        ServiceRequest project = memberProject(requestId,userId,communityId);
        InteriorPaymentMilestone milestone = paymentMilestoneRepository.findByIdAndServiceRequestId(milestoneId,requestId)
                .orElseThrow(() -> new ResourceNotFoundException("InteriorPaymentMilestone",milestoneId));
        if (!Objects.equals(project.getRequester().getId(),userId) && !Objects.equals(milestone.getProvider().getUser().getId(),userId)) {
            throw new UnauthorizedActionException("Only the project parties can attach its invoice");
        }
        milestone.setInvoiceReference(input.invoiceReference());
        milestone.setInvoiceUrl(input.invoiceUrl());
        return toPaymentMilestone(paymentMilestoneRepository.save(milestone));
    }

    @Transactional
    public CredentialResponse submitCredential(Long userId, CredentialCreate input) {
        ServiceProvider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider",userId));
        MediaObject evidence=mediaRepository.findByExternalIdAndDeletedFalse(input.evidenceMediaId())
                .orElseThrow(() -> new ResourceNotFoundException("CredentialEvidence",input.evidenceMediaId().toString()));
        if (evidence.getModule()!=MediaModule.INTERIORS || !String.valueOf(provider.getId()).equals(evidence.getModuleId())
                || !Objects.equals(evidence.getUploadedBy(),userId)) {
            throw new UnauthorizedActionException("Credential evidence must be uploaded by you to your provider profile");
        }
        ServiceProviderCredential credential=credentialRepository.save(ServiceProviderCredential.builder().provider(provider)
                .credentialType(input.credentialType().trim()).credentialLevel(input.credentialLevel().trim())
                .issuer(input.issuer()).credentialNumber(input.credentialNumber()).evidenceMediaId(input.evidenceMediaId())
                .submittedBy(userId).build());
        credentialAuditRepository.save(ServiceProviderCredentialAudit.builder().credential(credential).actorUserId(userId)
                .action("SUBMITTED").toStatus("PENDING").note("Credential evidence submitted").build());
        return toCredential(credential);
    }

    @Transactional(readOnly = true)
    public List<CredentialResponse> listMyCredentials(Long userId) {
        ServiceProvider provider=providerRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("ServiceProvider",userId));
        return credentialRepository.findByProviderIdOrderByCreatedAtDesc(provider.getId()).stream().map(this::toCredential).toList();
    }

    @Transactional(readOnly = true)
    public List<CredentialResponse> listPendingCredentials() {
        return credentialRepository.findByStatusOrderByCreatedAtAsc("PENDING").stream().map(this::toCredential).toList();
    }

    @Transactional(readOnly = true)
    public List<CredentialAuditResponse> credentialAudit(Long credentialId, Long userId) {
        ServiceProviderCredential credential=credentialRepository.findByIdAndProviderUserId(credentialId,userId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProviderCredential",credentialId));
        return credentialAuditRepository.findByCredentialIdOrderByCreatedAtAsc(credential.getId()).stream()
                .map(a -> new CredentialAuditResponse(a.getId(),a.getActorUserId(),a.getAction(),a.getFromStatus(),a.getToStatus(),a.getNote(),a.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<CredentialAuditResponse> credentialAuditForReview(Long credentialId) {
        if (!credentialRepository.existsById(credentialId)) throw new ResourceNotFoundException("ServiceProviderCredential",credentialId);
        return credentialAuditRepository.findByCredentialIdOrderByCreatedAtAsc(credentialId).stream()
                .map(a -> new CredentialAuditResponse(a.getId(),a.getActorUserId(),a.getAction(),a.getFromStatus(),a.getToStatus(),a.getNote(),a.getCreatedAt())).toList();
    }

    @Transactional
    public CredentialResponse reviewCredential(Long credentialId, Long reviewerId, CredentialReview input) {
        ServiceProviderCredential credential=credentialRepository.lockById(credentialId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProviderCredential",credentialId));
        if (!"PENDING".equals(credential.getStatus())) throw new IllegalStateException("Credential has already been reviewed");
        String decision=input.decision().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVED","REJECTED").contains(decision)) throw new IllegalArgumentException("Decision must be APPROVED or REJECTED");
        credential.setStatus(decision); credential.setReviewedBy(reviewerId); credential.setReviewNote(input.note());
        credential.setReviewedAt(java.time.LocalDateTime.now());
        credentialAuditRepository.save(ServiceProviderCredentialAudit.builder().credential(credential).actorUserId(reviewerId)
                .action("REVIEWED").fromStatus("PENDING").toStatus(decision).note(input.note()).build());
        if ("APPROVED".equals(decision)) {
            String existing=credential.getProvider().getVerifiedCredentialLevel();
            if (credentialRank(credential.getCredentialLevel())>=credentialRank(existing)) {
                credential.getProvider().setVerifiedCredentialLevel(credential.getCredentialLevel());
                credential.getProvider().setCredentialsVerifiedAt(java.time.LocalDateTime.now());
            }
            providerRepository.save(credential.getProvider());
        }
        return toCredential(credentialRepository.save(credential));
    }

    private int credentialRank(String level) {
        if(level==null) return 0;
        return switch(level.trim().toUpperCase(Locale.ROOT)) {
            case "STUDENT","ENTRY" -> 1;
            case "ASSOCIATE" -> 2;
            case "LICENSED" -> 3;
            case "CERTIFIED","PROFESSIONAL" -> 4;
            case "SENIOR" -> 5;
            case "MASTER","CHARTERED" -> 6;
            default -> 1;
        };
    }

    @Transactional
    public ProjectResponse updateProjectAttachments(Long requestId, Long userId, Long communityId, ProjectAttachmentsRequest request) {
        ServiceRequest project = ownedProject(requestId, userId, communityId);
        List<AttachmentRef> attachments = (request.mediaIds() == null ? List.<String>of() : request.mediaIds()).stream()
                .map(rawId -> {
                    UUID externalId;
                    try { externalId = UUID.fromString(rawId); }
                    catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid attachment reference"); }
                    MediaObject media = mediaRepository.findByExternalIdAndDeletedFalse(externalId)
                            .orElseThrow(() -> new ResourceNotFoundException("ProjectAttachment", rawId));
                    if (media.getModule() != MediaModule.INTERIORS
                            || !String.valueOf(requestId).equals(media.getModuleId())
                            || !Objects.equals(media.getUploadedBy(), userId)) {
                        throw new UnauthorizedActionException("You cannot attach this file to the interiors project");
                    }
                    return new AttachmentRef(media.getExternalId().toString(), media.getOriginalFileName(), media.getMimeType(), media.getFileSize());
                }).toList();
        Map<String, Object> details = new LinkedHashMap<>(readDetails(project.getCustomFieldValues()));
        details.put("attachments", attachments);
        project.setCustomFieldValues(writeJson(details));
        requestRepository.save(project);
        return toProject(project, null);
    }

    @Transactional
    public CommerceOrderDto checkoutProjectMaterials(Long requestId, AppUser buyer, Long communityId, CommerceCheckoutRequest checkout) {
        ownedProject(requestId, buyer.getId(), communityId);
        if (checkout.getItems() == null || checkout.getItems().isEmpty()) {
            throw new IllegalArgumentException("Choose at least one product for the project");
        }
        checkout.setChannel(CommerceChannel.MARKETPLACE);
        checkout.setChannelReferenceId("INTERIORS_PROJECT_" + requestId);
        return commerceCoreService.checkout(buyer, checkout);
    }

    @Transactional
    public CommerceOrderDto checkoutProjectBoq(Long requestId, AppUser buyer, Long communityId) {
        ServiceRequest project=ownedProject(requestId,buyer.getId(),communityId);
        if (project.getAssignedProvider()==null) throw new IllegalStateException("Accept a designer quote before ordering its BOQ materials");
        ServiceQuote accepted=quoteRepository.findByServiceRequestIdAndProviderId(requestId,project.getAssignedProvider().getId())
                .filter(quote -> "ACCEPTED".equals(quote.getStatus()))
                .orElseThrow(() -> new IllegalStateException("The accepted designer quote is unavailable"));
        List<InteriorQuoteLineItem> lines=quoteLineItemRepository.findByQuoteIdOrderByPositionAsc(accepted.getId()).stream()
                .filter(line -> line.getCatalogProductId()!=null && line.getProcurementQuantity()!=null).toList();
        if (lines.isEmpty()) throw new IllegalStateException("The accepted BOQ has no catalog products mapped for procurement");
        CommerceCheckoutRequest checkout=new CommerceCheckoutRequest();
        checkout.setChannel(CommerceChannel.MARKETPLACE);
        checkout.setChannelReferenceId("INTERIORS_PROJECT_"+requestId);
        checkout.setFulfillmentType("CLUBHOUSE_PICKUP");
        checkout.setPaymentMethod("UPI");
        checkout.setItems(lines.stream().map(line -> CommerceOrderItemDto.builder()
                .productId(String.valueOf(line.getCatalogProductId())).quantity(line.getProcurementQuantity()).build()).toList());
        return commerceCoreService.checkout(buyer,checkout);
    }

    @Transactional
    public ProjectResponse approveMilestone(Long requestId, String milestoneId, Long userId, Long communityId) {
        ServiceRequest project = ownedProject(requestId, userId, communityId);
        WorkOrder workOrder = workOrderRepository.findByServiceRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Milestones are available after a designer is assigned"));
        boolean completed = false;
        try {
            List<Map<String, Object>> tasks = objectMapper.readValue(workOrder.getChecklistItems(), new TypeReference<>() {});
            completed = tasks.stream().anyMatch(task -> milestoneId.equals(String.valueOf(task.get("id")))
                    && Boolean.parseBoolean(String.valueOf(task.get("done"))));
        } catch (Exception ignored) { }
        if (!completed) throw new IllegalStateException("Only completed project milestones can be approved");
        Map<String, Object> details = new LinkedHashMap<>(readDetails(project.getCustomFieldValues()));
        List<String> approvals = new ArrayList<>(listValue(details, "milestoneApprovals"));
        if (!approvals.contains(milestoneId)) approvals.add(milestoneId);
        details.put("milestoneApprovals", approvals);
        project.setCustomFieldValues(writeJson(details));
        requestRepository.save(project);
        return toProject(project, null);
    }

    private ServiceRequest ownedProject(Long requestId, Long userId, Long communityId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("InteriorProject", requestId));
        if (!"interior-works".equals(request.getCategory().getSlug())
                || !Objects.equals(request.getRequester().getId(), userId)
                || request.getCommunity() == null
                || !Objects.equals(request.getCommunity().getId(), communityId)) {
            throw new UnauthorizedActionException("You cannot access this interiors project");
        }
        return request;
    }

    private ServiceRequest memberProject(Long requestId, Long userId, Long communityId) {
        ServiceRequest request=requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("InteriorProject",requestId));
        boolean owner=Objects.equals(request.getRequester().getId(),userId);
        boolean assignedDesigner=request.getAssignedProvider()!=null && request.getAssignedProvider().getUser()!=null
                && Objects.equals(request.getAssignedProvider().getUser().getId(),userId);
        if (!"interior-works".equals(request.getCategory().getSlug()) || (!owner && !assignedDesigner)
                || request.getCommunity()==null || !Objects.equals(request.getCommunity().getId(),communityId)) {
            throw new UnauthorizedActionException("You cannot access this interiors project");
        }
        return request;
    }

    private void savePaymentEvent(InteriorPaymentMilestone milestone,Long actorId,String event,String reference) {
        paymentEventRepository.save(InteriorPaymentEvent.builder().milestone(milestone).actorUserId(actorId)
                .eventType(event).reference(reference).build());
    }

    private boolean validMilestonePayment(String status,Long paidBy,String refType,Long refId,BigDecimal paidAmount,
            Long userId,Long milestoneId,BigDecimal expectedAmount){
        return "PAID".equals(status) && Objects.equals(paidBy,userId) && "INTERIORS_MILESTONE".equals(refType)
                && Objects.equals(refId,milestoneId) && paidAmount!=null && paidAmount.compareTo(expectedAmount)==0;
    }

    private InspirationResponse toInspiration(InteriorSavedInspiration saved) {
        return new InspirationResponse(saved.getId(),saved.getServiceRequest()==null?null:saved.getServiceRequest().getId(),
                saved.getSourceUrl(),saved.getImageUrl(),saved.getTitle(),saved.getNotes(),saved.getTags(),saved.getCreatedAt(),saved.getUpdatedAt());
    }

    private ChangeRequestResponse toChangeRequest(InteriorChangeRequest request) {
        return new ChangeRequestResponse(request.getId(),request.getServiceRequest().getId(),request.getProposedBy(),request.getDescription(),
                request.getScopeDelta(),request.getPriceDelta(),request.getProposedStartDate(),request.getProposedEndDate(),request.getStatus(),
                request.getDecisionNote(),request.getDecidedBy(),request.getDecidedAt(),request.getCreatedAt());
    }

    private PaymentMilestoneResponse toPaymentMilestone(InteriorPaymentMilestone milestone) {
        return new PaymentMilestoneResponse(milestone.getId(),milestone.getServiceRequest().getId(),milestone.getProvider().getId(),
                milestone.getTitle(),milestone.getDescription(),milestone.getAmount(),milestone.getDueDate(),milestone.getStatus(),milestone.getPaymentGateway(),
                milestone.getRazorpayOrderId(),milestone.getRazorpayPaymentId(),milestone.getCashfreeManaOrderId(),milestone.getAuthorizedAt(),milestone.getReleasedAt(),
                milestone.getPayoutReference(),milestone.getInvoiceReference(),milestone.getInvoiceUrl());
    }

    private CredentialResponse toCredential(ServiceProviderCredential credential) {
        return new CredentialResponse(credential.getId(),credential.getProvider().getId(),credential.getCredentialType(),credential.getCredentialLevel(),
                credential.getIssuer(),credential.getCredentialNumber(),credential.getEvidenceMediaId(),credential.getStatus(),credential.getSubmittedBy(),
                credential.getReviewedBy(),credential.getReviewNote(),credential.getReviewedAt(),credential.getCreatedAt());
    }

    private ProjectResponse toProject(ServiceRequest request, Long providerId) {
        Map<String, Object> data = readDetails(request.getCustomFieldValues());
        List<Long> invitedIds = invitationRepository.findByServiceRequestId(request.getId()).stream()
                .map(invitation -> invitation.getProvider().getId()).toList();
        List<QuoteResponse> quotes = quoteRepository.findByServiceRequestIdOrderByCreatedAtDesc(request.getId()).stream()
                .filter(quote -> providerId == null || Objects.equals(quote.getProvider().getId(), providerId))
                .map(this::toQuote).toList();
        WorkOrderResponse workOrder = workOrderRepository.findByServiceRequestId(request.getId()).map(this::toWorkOrderResponse).orElse(null);
        ReviewResponse review = workOrder == null ? null : reviewRepository.findByWorkOrderId(workOrder.getId()).map(saved -> new ReviewResponse(
                saved.getId(), saved.getWorkOrder().getId(), saved.getProvider().getId(), saved.getProvider().getBusinessName(),
                saved.getReviewer().getId(), saved.getReviewer().getFullName(), saved.getRating(), saved.getQualityRating(),
                saved.getPunctualityRating(), saved.getBehaviorRating(), saved.getComment(), saved.getProviderResponse(),
                saved.isVerifiedResident(), saved.getCreatedAt())).orElse(null);
        return new ProjectResponse(
                request.getId(), request.getTitle(), request.getStatus().name(), stringValue(data, "propertyType"),
                stringValue(data, "homeSize"), stringValue(data, "locality"), stringValue(data, "stylePreference"),
                listValue(data, "focusAreas"), decimalValue(data, "budgetMin"),
                decimalValue(data, "budgetMax"), stringValue(data, "projectScope"), dateValue(data, "preferredStart"),
                request.getCreatedAt(), invitedIds, quotes, workOrder, review, attachmentValue(data), listValue(data, "milestoneApprovals"),
                providerId == null ? commerceCoreService.getOrdersByChannelReferenceId(request.getRequester(), "INTERIORS_PROJECT_" + request.getId()) : List.of());
    }

    private WorkOrderResponse toWorkOrderResponse(WorkOrder order) {
        String providerName = order.getProvider().getBusinessName();
        if (providerName == null || providerName.isBlank()) {
            providerName = order.getProvider().getUser() != null ? order.getProvider().getUser().getFullName() : "Interior designer";
        }
        return WorkOrderResponse.builder().id(order.getId()).serviceRequestId(order.getServiceRequest().getId())
                .providerId(order.getProvider().getId()).providerName(providerName).status(order.getStatus().name())
                .scheduledStart(order.getScheduledStart()).scheduledEnd(order.getScheduledEnd())
                .actualStart(order.getActualStart()).actualEnd(order.getActualEnd()).notes(order.getNotes())
                .checklistItems(order.getChecklistItems()).materialsUsed(order.getMaterialsUsed())
                .beforePhotos(order.getBeforePhotos()).afterPhotos(order.getAfterPhotos())
                .residentSignoff(order.isResidentSignoff()).residentSignoffAt(order.getResidentSignoffAt())
                .providerSignoff(order.isProviderSignoff()).providerSignoffAt(order.getProviderSignoffAt())
                .invoiceId(order.getInvoice() == null ? null : order.getInvoice().getId())
                .createdAt(order.getCreatedAt()).updatedAt(order.getUpdatedAt()).build();
    }

    private QuoteResponse toQuote(ServiceQuote quote) {
        String providerName = quote.getProvider().getBusinessName();
        if (providerName == null || providerName.isBlank()) {
            providerName = quote.getProvider().getUser() != null ? quote.getProvider().getUser().getFullName() : "Interior designer";
        }
        List<QuoteLineItemResponse> lineItems = quoteLineItemRepository.findByQuoteIdOrderByPositionAsc(quote.getId()).stream()
                .map(item -> new QuoteLineItemResponse(item.getId(), item.getCategory(), item.getDescription(), item.getQuantity(),
                        item.getUnit(), item.getUnitPrice(), item.getMarginPercent(), item.getTaxPercent(), item.getLineTotal(), item.getPosition(),
                        item.getCatalogProductId(),item.getProcurementQuantity()))
                .toList();
        return new QuoteResponse(quote.getId(), quote.getServiceRequest().getId(), quote.getProvider().getId(), providerName,
                quote.getQuotedAmount(), quote.getEstimatedDurationDays(), quote.getMaterialAllowance(), quote.getLaborAmount(),
                quote.getTaxAmount(), quote.getNotes(), quote.getStatus(), quote.getCreatedAt(), lineItems);
    }

    private List<InteriorQuoteLineItem> buildLineItems(List<QuoteLineItemRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();
        List<InteriorQuoteLineItem> result = new ArrayList<>();
        for (int position = 0; position < requests.size(); position++) {
            QuoteLineItemRequest item = requests.get(position);
            BigDecimal quantity = item.quantity();
            BigDecimal base = quantity.multiply(item.unitPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal marginPercent = zeroIfNull(item.marginPercent());
            BigDecimal taxPercent = zeroIfNull(item.taxPercent());
            BigDecimal margin = base.multiply(marginPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal taxable = base.add(margin);
            BigDecimal tax = taxable.multiply(taxPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (item.catalogProductId()!=null) {
                CommerceProduct product=productRepository.findById(item.catalogProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("CommerceProduct",item.catalogProductId()));
                if (!product.isActive()) throw new IllegalArgumentException("A mapped procurement product is inactive");
                if (item.procurementQuantity()==null) throw new IllegalArgumentException("Mapped BOQ items require an integer procurement quantity");
            } else if (item.procurementQuantity()!=null) {
                throw new IllegalArgumentException("A procurement quantity requires a catalog product mapping");
            }
            result.add(InteriorQuoteLineItem.builder().category(item.category().trim()).description(item.description().trim())
                    .quantity(quantity).unit(item.unit().trim()).unitPrice(item.unitPrice()).marginPercent(marginPercent)
                    .taxPercent(taxPercent).lineTotal(taxable.add(tax)).position(position)
                    .catalogProductId(item.catalogProductId()).procurementQuantity(item.procurementQuantity()).build());
        }
        return result;
    }

    private DesignerProfile toDesignerProfile(ServiceProviderResponse provider) {
        List<ServiceOfferingResponse> offerings = provider.getOfferings() == null ? List.of() : provider.getOfferings().stream()
                .filter(this::isInteriorOffering).toList();
        return new DesignerProfile(provider.getId(), provider.getUserId(), provider.getUserName(), provider.getProviderType(),
                provider.getBusinessName(), provider.getBio(), provider.getProfileImageUrl(), provider.getVerificationStatus(),
                provider.getAvgRating(), provider.getTotalJobsCompleted(), provider.getServiceAreas(), provider.getPortfolio(),
                provider.getVerifiedCredentialLevel(),provider.getCredentialsVerifiedAt(),offerings);
    }

    private List<ServiceCategory> interiorCategories() {
        return List.of(INTERIOR_CATEGORY, RENOVATION_CATEGORY).stream()
                .map(categoryRepository::findBySlugAndActiveTrue).flatMap(Optional::stream).toList();
    }

    private boolean isInteriorOffering(ServiceOfferingResponse offering) {
        return offering.getCategoryName() != null && (offering.getCategoryName().toLowerCase(Locale.ROOT).contains("interior")
                || offering.getCategoryName().toLowerCase(Locale.ROOT).contains("renovation"));
    }

    private boolean profileMatches(ServiceProviderResponse profile, String query) {
        String normalized = query.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(profile.getBusinessName(), normalized) || containsIgnoreCase(profile.getUserName(), normalized)
                || containsIgnoreCase(profile.getBio(), normalized) || profile.getOfferings().stream()
                .anyMatch(offering -> containsIgnoreCase(offering.getTitle(), normalized) || containsIgnoreCase(offering.getDescription(), normalized));
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Could not serialize the interiors project brief", ex); }
    }

    private Map<String, Object> readDetails(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (JsonProcessingException ex) { return Map.of(); }
    }

    private String stringValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private List<AttachmentRef> attachmentValue(Map<String, Object> values) {
        Object raw = values.get("attachments");
        if (!(raw instanceof List<?> items)) return List.of();
        return items.stream().filter(Map.class::isInstance).map(Map.class::cast).map(item -> new AttachmentRef(
                stringValue(item, "mediaId"), stringValue(item, "fileName"), stringValue(item, "contentType"),
                item.get("sizeBytes") == null ? null : Long.valueOf(String.valueOf(item.get("sizeBytes"))))).toList();
    }

    private List<String> listValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof List<?> items)) return List.of();
        return items.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    private BigDecimal decimalValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) return null;
        try { return new BigDecimal(String.valueOf(value)); }
        catch (NumberFormatException ex) { return null; }
    }

    private java.time.LocalDate dateValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return java.time.LocalDate.parse(String.valueOf(value)); }
        catch (java.time.format.DateTimeParseException ex) { return null; }
    }

    private BigDecimal zeroIfNull(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }
}
