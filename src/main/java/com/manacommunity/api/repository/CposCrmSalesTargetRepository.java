package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposCrmSalesTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CposCrmSalesTargetRepository extends JpaRepository<CposCrmSalesTarget, Long> {
    List<CposCrmSalesTarget> findByCommunityIdAndPeriodMonthAndPeriodYear(Long communityId, int month, int year);
    Optional<CposCrmSalesTarget> findByCommunityIdAndAgentNameAndPeriodMonthAndPeriodYear(
            Long communityId, String agentName, int month, int year);
}
