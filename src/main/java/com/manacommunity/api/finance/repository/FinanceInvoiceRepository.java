package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinanceInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceInvoiceRepository extends JpaRepository<FinanceInvoice, Long> {
    List<FinanceInvoice> findAllByOrderByDocDateDesc();
    List<FinanceInvoice> findByStatusOrderByDocDateDesc(String status);
}
