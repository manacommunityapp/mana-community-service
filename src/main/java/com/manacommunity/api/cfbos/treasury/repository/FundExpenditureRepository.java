package com.manacommunity.api.cfbos.treasury.repository;

import com.manacommunity.api.cfbos.treasury.entity.FundExpenditure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundExpenditureRepository extends JpaRepository<FundExpenditure, Long> {
    List<FundExpenditure> findByFundAccountId(Long fundAccountId);
}
