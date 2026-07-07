package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinancePurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancePurchaseRepository extends JpaRepository<FinancePurchase, Long> {
    List<FinancePurchase> findAllByOrderByDocDateDesc();
    List<FinancePurchase> findByStatusOrderByDocDateDesc(String status);
}
