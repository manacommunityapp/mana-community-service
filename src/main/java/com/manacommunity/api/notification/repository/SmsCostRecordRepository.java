package com.manacommunity.api.notification.repository;

import com.manacommunity.api.notification.entity.SmsCostRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface SmsCostRecordRepository extends JpaRepository<SmsCostRecord, Long> {

    @Query("""
            SELECT COALESCE(SUM(r.costUsd), 0)
            FROM SmsCostRecord r
            WHERE r.billingDate BETWEEN :from AND :to
            """)
    BigDecimal sumCostBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT COALESCE(SUM(r.costUsd), 0)
            FROM SmsCostRecord r
            WHERE r.communityId = :communityId
              AND r.billingDate BETWEEN :from AND :to
            """)
    BigDecimal sumCostByCommunityAndPeriod(
            @Param("communityId") Long communityId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT COALESCE(SUM(r.costUsd), 0)
            FROM SmsCostRecord r
            WHERE r.billingDate = :date
            """)
    BigDecimal sumCostByDate(@Param("date") LocalDate date);
}
