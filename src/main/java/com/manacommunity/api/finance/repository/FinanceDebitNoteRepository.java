package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinanceDebitNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceDebitNoteRepository extends JpaRepository<FinanceDebitNote, Long> {
    List<FinanceDebitNote> findAllByOrderByDocDateDesc();
    List<FinanceDebitNote> findByStatusOrderByDocDateDesc(String status);
}
