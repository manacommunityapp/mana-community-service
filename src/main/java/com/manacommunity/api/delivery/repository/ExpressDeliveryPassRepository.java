package com.manacommunity.api.delivery.repository;

import com.manacommunity.api.delivery.model.DeliveryPartner;
import com.manacommunity.api.delivery.model.ExpressDeliveryPass;
import com.manacommunity.api.delivery.model.ExpressPassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpressDeliveryPassRepository extends JpaRepository<ExpressDeliveryPass, Long> {
    Optional<ExpressDeliveryPass> findByEntryPassCode(String entryPassCode);
    Optional<ExpressDeliveryPass> findByBarcodePayload(String barcodePayload);
    Optional<ExpressDeliveryPass> findByPartnerAndOrderId(DeliveryPartner partner, String orderId);
    List<ExpressDeliveryPass> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, ExpressPassStatus status);
    List<ExpressDeliveryPass> findByCommunityIdAndFlatNumberOrderByCreatedAtDesc(Long communityId, String flatNumber);
}
