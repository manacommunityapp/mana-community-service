package com.manacommunity.api.fresh.dispatch.repository;

import com.manacommunity.api.fresh.dispatch.model.FreshVendorSettlement;
import com.manacommunity.api.fresh.dispatch.model.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FreshVendorSettlementRepository extends JpaRepository<FreshVendorSettlement, Long> {
    Optional<FreshVendorSettlement> findBySettlementReference(String settlementReference);
    List<FreshVendorSettlement> findByVendorIdOrderBySettlementPeriodEndDesc(Long vendorId);
    List<FreshVendorSettlement> findByStatus(SettlementStatus status);
}