package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinanceEstimate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceEstimateRepository extends JpaRepository<FinanceEstimate, Long> {
    List<FinanceEstimate> findAllByOrderByDocDateDesc();
    List<FinanceEstimate> findByStatusOrderByDocDateDesc(String status);
}
