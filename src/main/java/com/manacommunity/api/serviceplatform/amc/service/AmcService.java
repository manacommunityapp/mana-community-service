package com.manacommunity.api.serviceplatform.amc.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.serviceplatform.amc.dto.AmcPlanDto;
import com.manacommunity.api.serviceplatform.amc.dto.AmcSubscriptionDto;
import com.manacommunity.api.serviceplatform.amc.dto.ServiceWarrantyDto;
import com.manacommunity.api.serviceplatform.amc.engine.AmcWarrantyEngine;
import com.manacommunity.api.serviceplatform.amc.entity.AmcPlan;
import com.manacommunity.api.serviceplatform.amc.entity.AmcSubscription;
import com.manacommunity.api.serviceplatform.amc.entity.AmcSubscriptionStatus;
import com.manacommunity.api.serviceplatform.amc.entity.ServiceWarranty;
import com.manacommunity.api.serviceplatform.amc.repository.AmcPlanRepository;
import com.manacommunity.api.serviceplatform.amc.repository.AmcSubscriptionRepository;
import com.manacommunity.api.serviceplatform.amc.repository.ServiceWarrantyRepository;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.repository.ServiceCategoryRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmcService {

    private final AmcPlanRepository amcPlanRepository;
    private final AmcSubscriptionRepository amcSubscriptionRepository;
    private final ServiceWarrantyRepository serviceWarrantyRepository;
    private final ServiceProviderRepository providerRepository;
    private final ServiceCategoryRepository categoryRepository;
    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AmcWarrantyEngine engine;

    @Transactional
    public AmcPlanDto createPlan(AmcPlanDto dto) {
        ServiceProvider provider = providerRepository.findById(dto.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", dto.getProviderId()));
        ServiceCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceCategory", dto.getCategoryId()));

        AmcPlan plan = AmcPlan.builder()
                .provider(provider)
                .category(category)
                .name(dto.getName())
                .description(dto.getDescription())
                .planType(dto.getPlanType())
                .price(dto.getPrice())
                .visitsIncluded(dto.getVisitsIncluded())
                .durationMonths(dto.getDurationMonths())
                .terms(dto.getTerms())
                .active(true)
                .build();

        AmcPlan saved = amcPlanRepository.save(plan);
        return mapPlanToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AmcPlanDto> getPlansByProvider(Long providerId) {
        return amcPlanRepository.findByProviderIdAndActiveTrue(providerId).stream()
                .map(this::mapPlanToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AmcSubscriptionDto subscribe(Long planId, Long userId, Long communityId, LocalDate startDate) {
        AmcPlan plan = amcPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("AmcPlan", planId));
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", userId));
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

        if (startDate == null) {
            startDate = LocalDate.now();
        }

        AmcSubscription subscription = engine.createSubscription(plan, user, community, startDate);
        AmcSubscription saved = amcSubscriptionRepository.save(subscription);
        return mapSubscriptionToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AmcSubscriptionDto> getUserSubscriptions(Long userId) {
        return amcSubscriptionRepository.findByUserIdAndStatus(userId, AmcSubscriptionStatus.ACTIVE).stream()
                .map(this::mapSubscriptionToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AmcSubscriptionDto useVisit(Long subscriptionId) {
        AmcSubscription sub = amcSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("AmcSubscription", subscriptionId));

        if (!engine.canUseAmcVisit(sub, LocalDate.now())) {
            throw new IllegalStateException("AMC subscription is not active or has no remaining visits");
        }

        AmcSubscription updated = engine.consumeAmcVisit(sub);
        return mapSubscriptionToDto(amcSubscriptionRepository.save(updated));
    }

    @Transactional
    public ServiceWarrantyDto createWarrantyForWorkOrder(Long workOrderId, int warrantyDays, String terms) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));

        ServiceWarranty warranty = engine.calculateWarranty(workOrder, warrantyDays, terms);
        ServiceWarranty saved = serviceWarrantyRepository.save(warranty);
        return mapWarrantyToDto(saved);
    }

    @Transactional(readOnly = true)
    public ServiceWarrantyDto getWarrantyByWorkOrder(Long workOrderId) {
        ServiceWarranty warranty = serviceWarrantyRepository.findByWorkOrderId(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceWarranty", workOrderId));
        return mapWarrantyToDto(warranty);
    }

    private AmcPlanDto mapPlanToDto(AmcPlan p) {
        return AmcPlanDto.builder()
                .id(p.getId())
                .providerId(p.getProvider().getId())
                .providerName(p.getProvider().getBusinessName())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .name(p.getName())
                .description(p.getDescription())
                .planType(p.getPlanType())
                .price(p.getPrice())
                .visitsIncluded(p.getVisitsIncluded())
                .durationMonths(p.getDurationMonths())
                .terms(p.getTerms())
                .active(p.isActive())
                .build();
    }

    private AmcSubscriptionDto mapSubscriptionToDto(AmcSubscription s) {
        return AmcSubscriptionDto.builder()
                .id(s.getId())
                .amcPlanId(s.getAmcPlan().getId())
                .planName(s.getAmcPlan().getName())
                .userId(s.getUser().getId())
                .userName(s.getUser().getFullName())
                .communityId(s.getCommunity().getId())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .visitsRemaining(s.getVisitsRemaining())
                .visitsUsed(s.getVisitsUsed())
                .status(s.getStatus())
                .amountPaid(s.getAmountPaid())
                .build();
    }

    private ServiceWarrantyDto mapWarrantyToDto(ServiceWarranty w) {
        return ServiceWarrantyDto.builder()
                .id(w.getId())
                .workOrderId(w.getWorkOrder().getId())
                .warrantyPeriodDays(w.getWarrantyPeriodDays())
                .startDate(w.getStartDate())
                .endDate(w.getEndDate())
                .terms(w.getTerms())
                .status(w.getStatus())
                .build();
    }
}
