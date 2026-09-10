package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketGroupOrderParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketGroupOrderParticipantRepository extends JpaRepository<MarketGroupOrderParticipant, Long> {
    List<MarketGroupOrderParticipant> findByGroupOrderId(Long groupOrderId);
    Optional<MarketGroupOrderParticipant> findByGroupOrderIdAndUserId(Long groupOrderId, Long userId);
}
