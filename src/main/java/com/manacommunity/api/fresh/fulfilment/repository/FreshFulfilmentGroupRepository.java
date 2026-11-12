package com.manacommunity.api.fresh.fulfilment.repository;

import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentGroup;
import com.manacommunity.api.fresh.fulfilment.model.FulfilmentGroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshFulfilmentGroupRepository extends JpaRepository<FreshFulfilmentGroup, Long> {

    List<FreshFulfilmentGroup> findByOrderId(Long orderId);

    List<FreshFulfilmentGroup> findByVendorId(Long vendorId);

    List<FreshFulfilmentGroup> findByStatus(FulfilmentGroupStatus status);

    Optional<FreshFulfilmentGroup> findByGroupReference(String groupReference);

    Optional<FreshFulfilmentGroup> findByQrCodeToken(String qrCodeToken);
}
