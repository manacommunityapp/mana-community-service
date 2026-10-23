package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketCouponRepository extends JpaRepository<MarketCoupon, Long> {
    Optional<MarketCoupon> findByCodeIgnoreCaseAndActiveTrue(String code);
}
