package com.manacommunity.api.repository;

import com.manacommunity.api.model.BillingRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRunRepository extends JpaRepository<BillingRun, Long> {

    Optional<BillingRun> findByIdempotencyKey(String idempotencyKey);

    Optional<BillingRun> findByCommunityIdAndBillingMonthAndBillingYearAndRunType(
            Long communityId, Integer billingMonth, Integer billingYear, String runType);

    List<BillingRun> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<BillingRun> findByStatus(String status);

    List<BillingRun> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
}
