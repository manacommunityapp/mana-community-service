package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.*;
import com.manacommunity.api.commerce.core.model.CommerceCoupon;
import com.manacommunity.api.commerce.core.model.CommerceCouponUsage;
import com.manacommunity.api.commerce.core.repository.CommerceCouponRepository;
import com.manacommunity.api.commerce.core.repository.CommerceCouponUsageRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommerceCouponService {

    private final CommerceCouponRepository couponRepository;
    private final CommerceCouponUsageRepository usageRepository;
    private final AppUserRepository userRepository;

    // ── Admin CRUD ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CommerceCouponDto> getAdminCoupons(AppUser admin) {
        return couponRepository.findByCommunityIdOrderByCreatedAtDesc(admin.getCommunity().getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CommerceCouponDto createCoupon(AppUser admin, CommerceCouponRequest req) {
        AppUser vendor = req.getVendorUserId() != null
                ? userRepository.findById(req.getVendorUserId()).orElse(null) : null;

        CommerceCoupon coupon = CommerceCoupon.builder()
                .community(admin.getCommunity())
                .couponCode(req.getCouponCode().toUpperCase().trim())
                .title(req.getTitle())
                .description(req.getDescription())
                .couponType(req.getCouponType() != null ? req.getCouponType() : "PERCENT")
                .value(req.getValue())
                .minOrderAmount(req.getMinOrderAmount())
                .maxDiscount(req.getMaxDiscount())
                .vendorUser(vendor)
                .category(req.getCategory())
                .channel(req.getChannel())
                .maxUses(req.getMaxUses())
                .maxUsesPerUser(req.getMaxUsesPerUser() != null ? req.getMaxUsesPerUser() : 1)
                .validFrom(LocalDateTime.parse(req.getValidFrom()))
                .validTo(LocalDateTime.parse(req.getValidTo()))
                .active(req.getActive() == null || req.getActive())
                .createdBy(admin)
                .build();

        return toDto(couponRepository.save(coupon));
    }

    @Transactional
    public CommerceCouponDto updateCoupon(AppUser admin, Long id, CommerceCouponRequest req) {
        CommerceCoupon coupon = findForAdmin(admin, id);
        if (req.getTitle() != null) coupon.setTitle(req.getTitle());
        if (req.getMaxUses() != null) coupon.setMaxUses(req.getMaxUses());
        if (req.getValidTo() != null) coupon.setValidTo(LocalDateTime.parse(req.getValidTo()));
        if (req.getActive() != null) coupon.setActive(req.getActive());
        if (req.getDescription() != null) coupon.setDescription(req.getDescription());
        return toDto(couponRepository.save(coupon));
    }

    @Transactional
    public void deleteCoupon(AppUser admin, Long id) {
        CommerceCoupon coupon = findForAdmin(admin, id);
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    // ── Validate & apply (customer) ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CouponApplyResult validateCoupon(AppUser user, String code, BigDecimal orderAmount) {
        CommerceCoupon coupon = couponRepository
                .findByCommunityIdAndCouponCodeIgnoreCase(user.getCommunity().getId(), code)
                .orElse(null);

        if (coupon == null || !coupon.isActive()) {
            return CouponApplyResult.builder().valid(false).message("Coupon not found or inactive.").build();
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            return CouponApplyResult.builder().valid(false).message("Coupon is expired or not yet active.").build();
        }
        if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            return CouponApplyResult.builder().valid(false)
                    .message("Minimum order ₹" + coupon.getMinOrderAmount() + " required.").build();
        }
        if (coupon.getMaxUses() != null && usageRepository.countByCouponId(coupon.getId()) >= coupon.getMaxUses()) {
            return CouponApplyResult.builder().valid(false).message("Coupon usage limit reached.").build();
        }
        if (coupon.getMaxUsesPerUser() != null
                && usageRepository.countByCouponIdAndUserId(coupon.getId(), user.getId()) >= coupon.getMaxUsesPerUser()) {
            return CouponApplyResult.builder().valid(false).message("You have already used this coupon.").build();
        }

        BigDecimal discount = computeDiscount(coupon, orderAmount);
        BigDecimal finalAmt = orderAmount.subtract(discount).max(BigDecimal.ZERO);

        return CouponApplyResult.builder()
                .valid(true).message("Coupon applied successfully.")
                .discountAmount(discount).finalAmount(finalAmt)
                .couponCode(coupon.getCouponCode()).couponType(coupon.getCouponType())
                .build();
    }

    @Transactional
    public void recordUsage(AppUser user, String code, BigDecimal discount, String orderNumber) {
        CommerceCoupon coupon = couponRepository
                .findByCommunityIdAndCouponCodeIgnoreCase(user.getCommunity().getId(), code)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + code));
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
        usageRepository.save(CommerceCouponUsage.builder()
                .coupon(coupon).user(user).orderNumber(orderNumber).discountAmount(discount).build());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private BigDecimal computeDiscount(CommerceCoupon c, BigDecimal amount) {
        return switch (c.getCouponType().toUpperCase()) {
            case "PERCENT" -> {
                BigDecimal pct = amount.multiply(c.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                yield c.getMaxDiscount() != null ? pct.min(c.getMaxDiscount()) : pct;
            }
            case "FLAT" -> c.getValue().min(amount);
            default -> BigDecimal.ZERO;
        };
    }

    private CommerceCoupon findForAdmin(AppUser admin, Long id) {
        CommerceCoupon c = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + id));
        if (!c.getCommunity().getId().equals(admin.getCommunity().getId())) {
            throw new AccessDeniedException("Not in your community.");
        }
        return c;
    }

    private CommerceCouponDto toDto(CommerceCoupon c) {
        return CommerceCouponDto.builder()
                .id(c.getId()).couponCode(c.getCouponCode()).title(c.getTitle())
                .description(c.getDescription()).couponType(c.getCouponType()).value(c.getValue())
                .minOrderAmount(c.getMinOrderAmount()).maxDiscount(c.getMaxDiscount())
                .vendorUserId(c.getVendorUser() != null ? c.getVendorUser().getId() : null)
                .vendorName(c.getVendorUser() != null ? c.getVendorUser().getFullName() : null)
                .category(c.getCategory()).channel(c.getChannel())
                .maxUses(c.getMaxUses()).usedCount(c.getUsedCount())
                .maxUsesPerUser(c.getMaxUsesPerUser())
                .validFrom(c.getValidFrom().toString()).validTo(c.getValidTo().toString())
                .active(c.isActive())
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .build();
    }
}
