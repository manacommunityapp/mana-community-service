package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketHandoverPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketHandoverPassRepository extends JpaRepository<MarketHandoverPass, Long> {
    Optional<MarketHandoverPass> findByOrderId(Long orderId);
    Optional<MarketHandoverPass> findByPassQrCode(String passQrCode);
}
