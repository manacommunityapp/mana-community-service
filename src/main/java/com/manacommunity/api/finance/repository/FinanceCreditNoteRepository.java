package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinanceCreditNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceCreditNoteRepository extends JpaRepository<FinanceCreditNote, Long> {
    List<FinanceCreditNote> findAllByOrderByDocDateDesc();
    List<FinanceCreditNote> findByStatusOrderByDocDateDesc(String status);
}
