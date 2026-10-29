package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.PaymentGatewaySettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentGatewaySettlementRepository extends JpaRepository<PaymentGatewaySettlement, Long> {

    List<PaymentGatewaySettlement> findByCommunityIdOrderBySettlementDateDesc(Long communityId);

    List<PaymentGatewaySettlement> findByCommunityIdAndStatusOrderBySettlementDateDesc(Long communityId, String status);
}
