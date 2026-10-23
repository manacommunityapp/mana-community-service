package com.manacommunity.api.serviceplatform.amc.engine;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.serviceplatform.amc.entity.AmcPlan;
import com.manacommunity.api.serviceplatform.amc.entity.AmcSubscription;
import com.manacommunity.api.serviceplatform.amc.entity.AmcSubscriptionStatus;
import com.manacommunity.api.serviceplatform.amc.entity.ServiceWarranty;
import com.manacommunity.api.serviceplatform.amc.entity.WarrantyStatus;
import com.manacommunity.api.serviceplatform.entity.WorkOrder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AmcWarrantyEngine {

    public boolean isWarrantyValid(ServiceWarranty warranty, LocalDate checkDate) {
        if (warranty == null || warranty.getStatus() != WarrantyStatus.ACTIVE) {
            return false;
        }
        if (checkDate.isBefore(warranty.getStartDate()) || checkDate.isAfter(warranty.getEndDate())) {
            return false;
        }
        return true;
    }

    public boolean canUseAmcVisit(AmcSubscription subscription, LocalDate serviceDate) {
        if (subscription == null || subscription.getStatus() != AmcSubscriptionStatus.ACTIVE) {
            return false;
        }
        if (serviceDate.isBefore(subscription.getStartDate()) || serviceDate.isAfter(subscription.getEndDate())) {
            return false;
        }
        return subscription.getVisitsRemaining() > 0;
    }

    public AmcSubscription consumeAmcVisit(AmcSubscription subscription) {
        if (subscription.getVisitsRemaining() <= 0) {
            throw new IllegalStateException("No visits remaining in this AMC subscription");
        }
        subscription.setVisitsRemaining(subscription.getVisitsRemaining() - 1);
        subscription.setVisitsUsed(subscription.getVisitsUsed() + 1);
        if (subscription.getVisitsRemaining() == 0) {
            subscription.setStatus(AmcSubscriptionStatus.EXPIRED);
        }
        return subscription;
    }

    public ServiceWarranty calculateWarranty(WorkOrder workOrder, int warrantyDays, String terms) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(warrantyDays);
        return ServiceWarranty.builder()
                .workOrder(workOrder)
                .warrantyPeriodDays(warrantyDays)
                .startDate(startDate)
                .endDate(endDate)
                .terms(terms)
                .status(WarrantyStatus.ACTIVE)
                .build();
    }

    public AmcSubscription createSubscription(AmcPlan plan, AppUser user, Community community, LocalDate startDate) {
        LocalDate endDate = startDate.plusMonths(plan.getDurationMonths());
        return AmcSubscription.builder()
                .amcPlan(plan)
                .user(user)
                .community(community)
                .startDate(startDate)
                .endDate(endDate)
                .visitsRemaining(plan.getVisitsIncluded())
                .visitsUsed(0)
                .status(AmcSubscriptionStatus.ACTIVE)
                .amountPaid(plan.getPrice())
                .build();
    }
}
