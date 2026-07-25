package com.manacommunity.api.cfbos.accounting.repository;

import com.manacommunity.api.cfbos.accounting.entity.AccountingPeriod;
import com.manacommunity.api.cfbos.accounting.entity.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {
    List<AccountingPeriod> findByFiscalYearOrderByPeriodNumber(FiscalYear fiscalYear);

    @Query("SELECT p FROM AccountingPeriod p WHERE p.fiscalYear = :fy AND :date BETWEEN p.startDate AND p.endDate")
    Optional<AccountingPeriod> findByFiscalYearAndDate(FiscalYear fy, LocalDate date);
}
