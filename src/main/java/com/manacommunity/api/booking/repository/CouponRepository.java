package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeAndIsActiveTrue(String code);

    List<Coupon> findByCommunityIdAndIsActiveTrueOrderByCreatedAtDesc(Long communityId);
}
