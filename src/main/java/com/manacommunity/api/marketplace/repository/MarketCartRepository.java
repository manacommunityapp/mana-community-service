package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketCartRepository extends JpaRepository<MarketCart, Long> {
    Optional<MarketCart> findByUserId(Long userId);
}
