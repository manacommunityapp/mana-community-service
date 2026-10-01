package com.manacommunity.api.serviceplatform.unit;

import com.manacommunity.api.serviceplatform.analytics.dto.PlatformAnalyticsResponse;
import com.manacommunity.api.serviceplatform.analytics.engine.AnalyticsEngine;
import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Analytics Engine Unit Tests")
class AnalyticsEngineTest {

    private AnalyticsEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AnalyticsEngine();
    }

    @Test
    @DisplayName("Should compute platform metrics accurately")
    void testCalculateAnalytics() {
        ServiceCategory plumbing = ServiceCategory.builder().id(1L).name("Plumbing").build();

        ServiceRequest req1 = ServiceRequest.builder()
                .id(1L)
                .status(ServiceRequestStatus.COMPLETED)
                .category(plumbing)
                .actualCost(new BigDecimal("1500.00"))
                .build();

        List<ServiceRequest> requests = List.of(
                req1,
                ServiceRequest.builder().id(2L).status(ServiceRequestStatus.CANCELLED).category(plumbing).build()
        );

        LocalDateTime now = LocalDateTime.now();
        List<WorkOrder> workOrders = List.of(
                WorkOrder.builder()
                        .id(1L)
                        .serviceRequest(req1)
                        .status(WorkOrderStatus.COMPLETED)
                        .actualStart(now.minusHours(2))
                        .actualEnd(now)
                        .build()
        );

        PlatformAnalyticsResponse response = engine.calculateAnalytics(requests, workOrders, 4.8);

        assertNotNull(response);
        assertEquals(2, response.getTotalRequests());
        assertEquals(1, response.getCompletedWorkOrders());
        assertEquals(1, response.getCancelledRequests());
        assertEquals(new BigDecimal("1500.00"), response.getTotalRevenueGenerated());
        assertEquals(2.0, response.getAverageFulfillmentTimeHours());
        assertEquals(4.8, response.getOverallSatisfactionRating());
    }
}
