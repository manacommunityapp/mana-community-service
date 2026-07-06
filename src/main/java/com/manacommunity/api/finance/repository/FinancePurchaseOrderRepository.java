package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinancePurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancePurchaseOrderRepository extends JpaRepository<FinancePurchaseOrder, Long> {
    List<FinancePurchaseOrder> findAllByOrderByDocDateDesc();
    List<FinancePurchaseOrder> findByStatusOrderByDocDateDesc(String status);
}
