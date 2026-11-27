package com.manacommunity.api.procurement.repository;

import com.manacommunity.api.procurement.entity.ProcPurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcPurchaseOrderRepository extends JpaRepository<ProcPurchaseOrder, Long> {

    Optional<ProcPurchaseOrder> findByPoNumber(String poNumber);

    List<ProcPurchaseOrder> findByCommunityId(Long communityId);

    List<ProcPurchaseOrder> findByCommunityIdAndStatus(Long communityId, ProcPurchaseOrder.PoStatus status);

    List<ProcPurchaseOrder> findByCommunityIdAndCategory(Long communityId, String category);
}
