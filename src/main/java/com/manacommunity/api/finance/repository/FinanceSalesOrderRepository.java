package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinanceSalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceSalesOrderRepository extends JpaRepository<FinanceSalesOrder, Long> {
    List<FinanceSalesOrder> findAllByOrderByDocDateDesc();
    List<FinanceSalesOrder> findByStatusOrderByDocDateDesc(String status);
}
