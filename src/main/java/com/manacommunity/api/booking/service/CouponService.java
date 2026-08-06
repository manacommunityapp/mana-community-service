package com.manacommunity.api.booking.service;

import com.manacommunity.api.booking.dto.CouponRequest;
import com.manacommunity.api.booking.dto.CouponResponse;
import com.manacommunity.api.booking.entity.Coupon;
import com.manacommunity.api.booking.entity.enums.DiscountType;
import com.manacommunity.api.booking.repository.CouponRepository;
import com.manacommunity.api.model.Community;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepo;

    @Transactional(readOnly = true)
    public List<CouponResponse> getCoupons(Long communityId) {
        return couponRepo.findByCommunityIdAndIsActiveTrueOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CouponResponse createCoupon(CouponRequest req, Community community) {
        if (couponRepo.findByCodeAndIsActiveTrue(req.getCode()).isPresent()) {
            throw new IllegalStateException("Coupon code already exists: " + req.getCode());
        }

        Coupon coupon = Coupon.builder()
                .code(req.getCode().toUpperCase())
                .description(req.getDescription())
                .discountType(DiscountType.valueOf(req.getDiscountType()))
                .discountValue(req.getDiscountValue())
                .maxUses(req.getMaxUses())
                .currentUses(0)
                .validFrom(LocalDateTime.parse(req.getValidFrom()))
                .validTo(LocalDateTime.parse(req.getValidTo()))
                .minBookingAmount(req.getMinBookingAmount())
                .maxDiscountAmount(req.getMaxDiscountAmount())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .community(community)
                .build();

        return toResponse(couponRepo.save(coupon));
    }

    @Transactional(readOnly = true)
    public CouponResponse validateCoupon(String code) {
        Coupon coupon = couponRepo.findByCodeAndIsActiveTrue(code.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired coupon code"));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            throw new IllegalStateException("Coupon is not valid at this time");
        }
        if (coupon.getMaxUses() != null && coupon.getCurrentUses() >= coupon.getMaxUses()) {
            throw new IllegalStateException("Coupon has reached maximum usage limit");
        }

        return toResponse(coupon);
    }

    private CouponResponse toResponse(Coupon c) {
        return CouponResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .description(c.getDescription())
                .discountType(c.getDiscountType())
                .discountValue(c.getDiscountValue())
                .maxUses(c.getMaxUses())
                .currentUses(c.getCurrentUses())
                .validFrom(c.getValidFrom().toString())
                .validTo(c.getValidTo().toString())
                .minBookingAmount(c.getMinBookingAmount())
                .maxDiscountAmount(c.getMaxDiscountAmount())
                .isActive(c.isActive())
                .build();
    }
}
