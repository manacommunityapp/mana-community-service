package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceSettlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT s FROM CommerceSettlement s WHERE s.community.id = :communityId ORDER BY s.cycleEndDate DESC")
    Page<CommerceSettlement> findByCommunityIdPaged(Long communityId, Pageable pageable);

    @Query("SELECT s FROM CommerceSettlement s WHERE s.community.id = :communityId AND s.status = :status ORDER BY s.cycleEndDate DESC")
    Page<CommerceSettlement> findByCommunityIdAndStatusPaged(Long communityId, String status, Pageable pageable);
}