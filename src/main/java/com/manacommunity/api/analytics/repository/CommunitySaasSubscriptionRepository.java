package com.manacommunity.api.analytics.repository;

import com.manacommunity.api.analytics.entity.CommunitySaasSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CommunitySaasSubscriptionRepository extends JpaRepository<CommunitySaasSubscription, Long> {
    @Query("SELECT s FROM CommunitySaasSubscription s WHERE s.status = " +
            "com.manacommunity.api.analytics.entity.CommunitySaasSubscription.Status.ACTIVE " +
            "AND s.recurringAmount > 0 AND s.currentPeriodStart <= :asOf " +
            "AND (s.currentPeriodEnd IS NULL OR s.currentPeriodEnd >= :asOf)")
    List<CommunitySaasSubscription> findCurrentlyPaying(@Param("asOf") LocalDate asOf);
}
