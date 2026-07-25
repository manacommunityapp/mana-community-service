package com.manacommunity.api.cfbos.accounting.repository;

import com.manacommunity.api.cfbos.accounting.entity.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {
    Optional<FiscalYear> findByIsCurrentTrue();
    Optional<FiscalYear> findByName(String name);
}
