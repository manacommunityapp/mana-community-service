package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinanceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceDocumentRepository extends JpaRepository<FinanceDocument, Long> {
    List<FinanceDocument> findByTypeOrderByDocDateDesc(FinanceDocument.Type type);
    List<FinanceDocument> findAllByOrderByDocDateDesc();
}
