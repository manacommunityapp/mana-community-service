package com.manacommunity.api.cfbos.treasury.repository;

import com.manacommunity.api.cfbos.treasury.entity.FundContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundContributionRepository extends JpaRepository<FundContribution, Long> {
    List<FundContribution> findByFundAccountId(Long fundAccountId);
    List<FundContribution> findByFlatNumber(String flatNumber);
}
