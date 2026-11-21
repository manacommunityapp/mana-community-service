package com.manacommunity.api.analytics.service;

import com.manacommunity.api.analytics.dto.CommunitySubscriptionRequest;
import com.manacommunity.api.analytics.dto.CommunitySubscriptionResponse;
import com.manacommunity.api.analytics.entity.CommunitySaasSubscription;
import com.manacommunity.api.analytics.repository.CommunitySaasSubscriptionRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunitySubscriptionAdminService {
    private static final List<Integer> PERIODS = List.of(1, 3, 6, 12);

    private final CommunitySaasSubscriptionRepository subscriptionRepository;
    private final CommunityRepository communityRepository;

    @Transactional(readOnly = true)
    public List<CommunitySubscriptionResponse> list() {
        return subscriptionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CommunitySubscriptionResponse create(CommunitySubscriptionRequest request) {
        validate(request);
        Community community = communityRepository.findById(request.getCommunityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
        CommunitySaasSubscription subscription = new CommunitySaasSubscription();
        subscription.setCommunity(community);
        apply(subscription, request);
        try {
            return toResponse(subscriptionRepository.saveAndFlush(subscription));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The community already has an active subscription or the external reference is in use");
        }
    }

    @Transactional
    public CommunitySubscriptionResponse update(Long id, CommunitySubscriptionRequest request) {
        validate(request);
        CommunitySaasSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        if (!subscription.getCommunity().getId().equals(request.getCommunityId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A subscription cannot be moved to another community");
        }
        apply(subscription, request);
        try {
            return toResponse(subscriptionRepository.saveAndFlush(subscription));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The external reference is already in use");
        }
    }

    private void validate(CommunitySubscriptionRequest request) {
        if (!PERIODS.contains(request.getBillingPeriodMonths())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "billingPeriodMonths must be 1, 3, 6, or 12");
        }
        if (request.getCurrentPeriodEnd() != null && request.getCurrentPeriodEnd().isBefore(request.getCurrentPeriodStart())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currentPeriodEnd must be on or after currentPeriodStart");
        }
        if (request.getRecurringAmount().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurringAmount supports at most two decimal places");
        }
    }

    private void apply(CommunitySaasSubscription target, CommunitySubscriptionRequest request) {
        target.setExternalReference(request.getExternalReference());
        target.setPlanCode(request.getPlanCode().trim());
        target.setPlanName(request.getPlanName().trim());
        target.setRecurringAmount(request.getRecurringAmount());
        target.setBillingPeriodMonths(request.getBillingPeriodMonths());
        target.setStatus(request.getStatus());
        target.setCurrentPeriodStart(request.getCurrentPeriodStart());
        target.setCurrentPeriodEnd(request.getCurrentPeriodEnd());
    }

    private CommunitySubscriptionResponse toResponse(CommunitySaasSubscription s) {
        return CommunitySubscriptionResponse.builder().id(s.getId()).communityId(s.getCommunity().getId())
                .externalReference(s.getExternalReference()).planCode(s.getPlanCode()).planName(s.getPlanName())
                .recurringAmount(s.getRecurringAmount()).billingPeriodMonths(s.getBillingPeriodMonths())
                .status(s.getStatus()).currentPeriodStart(s.getCurrentPeriodStart()).currentPeriodEnd(s.getCurrentPeriodEnd()).build();
    }
}
