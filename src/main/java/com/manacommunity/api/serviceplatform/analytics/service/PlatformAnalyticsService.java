package com.manacommunity.api.serviceplatform.analytics.service;

import com.manacommunity.api.serviceplatform.analytics.dto.PlatformAnalyticsResponse;
import com.manacommunity.api.serviceplatform.analytics.engine.AnalyticsEngine;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.repository.ServiceRequestRepository;
import com.manacommunity.api.serviceplatform.repository.WorkOrderRepository;
import com.manacommunity.api.serviceplatform.review.repository.ServiceReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformAnalyticsService {

    private final ServiceRequestRepository requestRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ServiceReviewRepository reviewRepository;
    private final AnalyticsEngine engine;

    @Transactional(readOnly = true)
    public PlatformAnalyticsResponse getCommunityAnalytics(Long communityId) {
        List<ServiceRequest> requests = requestRepository.findByCommunityId(communityId);
        List<WorkOrder> workOrders = workOrderRepository.findByCommunityId(communityId);
        return engine.calculateAnalytics(requests, workOrders, 4.6);
    }
}
