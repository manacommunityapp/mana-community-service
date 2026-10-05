package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceSettlementRepository extends JpaRepository<CommerceSettlement, Long> {
    Optional<CommerceSettlement> findBySettlementNumber(String settlementNumber);
    List<CommerceSettlement> findByVendorIdOrderByCycleEndDateDesc(String vendorId);
    List<CommerceSettlement> findBySeller_IdOrderByCycleEndDateDesc(Long sellerId);
    List<CommerceSettlement> findBySeller_Id(Long sellerId);
    List<CommerceSettlement> findByCommunityIdOrderByCycleEndDateDesc(Long communityId);
}