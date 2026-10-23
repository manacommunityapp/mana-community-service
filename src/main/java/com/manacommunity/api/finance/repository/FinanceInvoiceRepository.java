package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinanceInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface FinanceInvoiceRepository extends JpaRepository<FinanceInvoice, Long> {
    List<FinanceInvoice> findAllByOrderByDocDateDesc();
    List<FinanceInvoice> findByStatusOrderByDocDateDesc(String status);

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM FinanceInvoice i WHERE i.status <> 'Paid'")
    BigDecimal sumOutstandingReceivables();
}
