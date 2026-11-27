package com.manacommunity.api.accounting.repository;

import com.manacommunity.api.accounting.entity.TaxRecalculationBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaxRecalculationBatchRepository extends JpaRepository<TaxRecalculationBatchEntity, UUID> {
}
