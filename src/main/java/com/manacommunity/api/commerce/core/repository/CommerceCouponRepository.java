package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceCouponRepository extends JpaRepository<CommerceCoupon, Long> {
    Optional<CommerceCoupon> findByCommunityIdAndCouponCodeIgnoreCase(Long communityId, String code);
    List<CommerceCoupon> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<CommerceCoupon> findByCommunityIdAndActiveTrueAndValidFromBeforeAndValidToAfter(
            Long communityId, LocalDateTime now1, LocalDateTime now2);
}
