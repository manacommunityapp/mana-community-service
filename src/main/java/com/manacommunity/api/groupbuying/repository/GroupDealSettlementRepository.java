package com.manacommunity.api.groupbuying.repository;

import com.manacommunity.api.groupbuying.model.GroupDealSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupDealSettlementRepository extends JpaRepository<GroupDealSettlement, Long> {
    List<GroupDealSettlement> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<GroupDealSettlement> findByVendorIdOrderByCreatedAtDesc(String vendorId);
    Optional<GroupDealSettlement> findByDealId(Long dealId);
}
