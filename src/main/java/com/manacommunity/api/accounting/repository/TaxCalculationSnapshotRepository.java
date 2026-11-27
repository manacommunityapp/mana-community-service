package com.manacommunity.api.accounting.repository;

import com.manacommunity.api.accounting.entity.TaxCalculationSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaxCalculationSnapshotRepository extends JpaRepository<TaxCalculationSnapshotEntity, UUID> {

    List<TaxCalculationSnapshotEntity> findBySocietyIdAndFyCodeAndRuleFamilyAndSupplyDateBetweenOrderBySupplyDateAsc(
            Long societyId, String fyCode, String ruleFamily, LocalDate fromDate, LocalDate toDate);
}
