package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.BrsLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BrsLineRepository extends JpaRepository<BrsLine, Long> {

    List<BrsLine> findBySessionIdOrderByValueDateAsc(Long sessionId);

    long countBySessionIdAndMatchStatus(Long sessionId, String matchStatus);

    @Query("SELECT COALESCE(SUM(l.creditAmount), 0) FROM BrsLine l WHERE l.session.id = :sessionId AND l.creditAmount IS NOT NULL")
    BigDecimal sumCreditsBySessionId(Long sessionId);

    @Query("SELECT COALESCE(SUM(l.debitAmount), 0) FROM BrsLine l WHERE l.session.id = :sessionId AND l.debitAmount IS NOT NULL")
    BigDecimal sumDebitsBySessionId(Long sessionId);
}
