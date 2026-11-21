package com.manacommunity.api.analytics.repository;

import com.manacommunity.api.analytics.entity.BusinessModuleFinanceDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BusinessModuleFinanceDailyRepository extends JpaRepository<BusinessModuleFinanceDaily, Long> {
    Optional<BusinessModuleFinanceDaily> findByModuleCodeAndReportDate(String moduleCode, LocalDate reportDate);

    @Query("SELECT f.moduleCode, SUM(f.commissionRevenue), SUM(f.directCost) FROM BusinessModuleFinanceDaily f " +
            "WHERE f.reportDate BETWEEN :from AND :to GROUP BY f.moduleCode")
    List<Object[]> aggregateForPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
