package com.manacommunity.api.procurement.repository;

import com.manacommunity.api.procurement.entity.ProcGoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcGoodsReceiptRepository extends JpaRepository<ProcGoodsReceipt, Long> {

    Optional<ProcGoodsReceipt> findByGrnNumber(String grnNumber);

    List<ProcGoodsReceipt> findByPurchaseOrderId(Long purchaseOrderId);

    List<ProcGoodsReceipt> findByCommunityId(Long communityId);
}
