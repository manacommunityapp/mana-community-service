package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.TdsChallan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TdsChallanRepository extends JpaRepository<TdsChallan, Long> {

    List<TdsChallan> findByCommunityIdOrderByFinancialYearDescQuarterDesc(Long communityId);

    List<TdsChallan> findByCommunityIdAndFinancialYearOrderByQuarterDesc(Long communityId, String financialYear);

    List<TdsChallan> findByCommunityIdAndFinancialYearAndQuarterOrderByTdsSectionAsc(
            Long communityId, String financialYear, String quarter);

    Optional<TdsChallan> findByCommunityIdAndFinancialYearAndQuarterAndTdsSection(
            Long communityId, String financialYear, String quarter, String tdsSection);
}
