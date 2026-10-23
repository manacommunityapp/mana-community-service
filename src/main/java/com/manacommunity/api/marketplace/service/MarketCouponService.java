package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketCouponDto;
import com.manacommunity.api.marketplace.entity.MarketCoupon;
import com.manacommunity.api.marketplace.repository.MarketCouponRepository;
import com.manacommunity.api.model.Community;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketCouponService {

    private final MarketCouponRepository couponRepository;

    public MarketCouponDto.Response validateCoupon(String code, BigDecimal subtotal) {
        MarketCoupon coupon = couponRepository.findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon code not found or expired"));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            throw new InvalidInputException("Coupon is expired or not yet active");
        }

        if (coupon.getUsageLimit() != null && coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            throw new InvalidInputException("Coupon usage limit has been reached");
        }

        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new InvalidInputException("Order subtotal must be at least " + coupon.getMinOrderAmount() + " to use this coupon");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == MarketCoupon.DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else if (coupon.getDiscountType() == MarketCoupon.DiscountType.FIXED) {
            discount = coupon.getDiscountValue();
        }

        return MarketCouponDto.Response.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .usageLimit(coupon.getUsageLimit())
                .timesUsed(coupon.getTimesUsed())
                .active(coupon.isActive())
                .calculatedDiscount(discount)
                .build();
    }

    @Transactional
    public MarketCouponDto.Response create(MarketCouponDto.Request req, Community community) {
        MarketCoupon coupon = MarketCoupon.builder()
                .code(req.getCode().toUpperCase().trim())
                .description(req.getDescription())
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .minOrderAmount(req.getMinOrderAmount())
                .maxDiscountAmount(req.getMaxDiscountAmount())
                .validFrom(req.getValidFrom())
                .validTo(req.getValidTo())
                .usageLimit(req.getUsageLimit())
                .timesUsed(0)
                .active(req.getActive() != null ? req.getActive() : true)
                .community(community)
                .build();

        MarketCoupon saved = couponRepository.save(coupon);

        return MarketCouponDto.Response.builder()
                .id(saved.getId())
                .code(saved.getCode())
                .description(saved.getDescription())
                .discountType(saved.getDiscountType())
                .discountValue(saved.getDiscountValue())
                .minOrderAmount(saved.getMinOrderAmount())
                .maxDiscountAmount(saved.getMaxDiscountAmount())
                .validFrom(saved.getValidFrom())
                .validTo(saved.getValidTo())
                .usageLimit(saved.getUsageLimit())
                .timesUsed(saved.getTimesUsed())
                .active(saved.isActive())
                .build();
    }
}
