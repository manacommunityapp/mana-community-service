package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.entity.MultiTenderSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MultiTenderSettlementRepository extends JpaRepository<MultiTenderSettlement, Long> {
    Optional<MultiTenderSettlement> findByOrderRef(String orderRef);
}
