package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinanceReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceReceiptRepository extends JpaRepository<FinanceReceipt, Long> {
    List<FinanceReceipt> findByReceiptTypeOrderByReceiptDateDesc(FinanceReceipt.ReceiptType type);
    List<FinanceReceipt> findAllByOrderByReceiptDateDesc();
}
