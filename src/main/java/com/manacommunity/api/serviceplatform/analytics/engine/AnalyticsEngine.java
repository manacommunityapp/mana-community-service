package com.manacommunity.api.serviceplatform.analytics.engine;

import com.manacommunity.api.serviceplatform.analytics.dto.PlatformAnalyticsResponse;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceRequestStatus;
import com.manacommunity.api.serviceplatform.entity.enums.WorkOrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnalyticsEngine {

    public PlatformAnalyticsResponse calculateAnalytics(
            List<ServiceRequest> requests,
            List<WorkOrder> workOrders,
            Double avgRating) {

        long totalRequests = requests.size();
        long completedWorkOrders = 0;
        long pendingWorkOrders = 0;
        long cancelledRequests = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        Map<String, Long> requestsByCategory = new HashMap<>();
        Map<String, Long> requestsByStatus = new HashMap<>();

        for (ServiceRequest req : requests) {
            String status = req.getStatus().name();
            requestsByStatus.put(status, requestsByStatus.getOrDefault(status, 0L) + 1);

            if (req.getStatus() == ServiceRequestStatus.CANCELLED) {
                cancelledRequests++;
            }

            if (req.getCategory() != null) {
                String cat = req.getCategory().getName();
                requestsByCategory.put(cat, requestsByCategory.getOrDefault(cat, 0L) + 1);
            }
        }

        double totalFulfillmentHours = 0.0;
        int completedCountWithTime = 0;

        for (WorkOrder wo : workOrders) {
            if (wo.getStatus() == WorkOrderStatus.COMPLETED) {
                completedWorkOrders++;
                if (wo.getServiceRequest() != null) {
                    if (wo.getServiceRequest().getActualCost() != null) {
                        totalRevenue = totalRevenue.add(wo.getServiceRequest().getActualCost());
                    } else if (wo.getServiceRequest().getEstimatedCost() != null) {
                        totalRevenue = totalRevenue.add(wo.getServiceRequest().getEstimatedCost());
                    }
                }
                if (wo.getActualStart() != null && wo.getActualEnd() != null) {
                    Duration d = Duration.between(wo.getActualStart(), wo.getActualEnd());
                    totalFulfillmentHours += d.toMinutes() / 60.0;
                    completedCountWithTime++;
                }
            } else if (wo.getStatus() != WorkOrderStatus.CANCELLED) {
                pendingWorkOrders++;
            }
        }

        double avgFulfillmentHours = completedCountWithTime > 0
                ? (totalFulfillmentHours / completedCountWithTime)
                : 2.5;

        return PlatformAnalyticsResponse.builder()
                .totalRequests(totalRequests)
                .completedWorkOrders(completedWorkOrders)
                .pendingWorkOrders(pendingWorkOrders)
                .cancelledRequests(cancelledRequests)
                .averageFulfillmentTimeHours(Math.round(avgFulfillmentHours * 10.0) / 10.0)
                .overallSatisfactionRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 4.5)
                .totalRevenueGenerated(totalRevenue)
                .requestsByCategory(requestsByCategory)
                .requestsByStatus(requestsByStatus)
                .build();
    }
}
