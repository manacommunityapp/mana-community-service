package com.manacommunity.api.serviceplatform.pricing.engine;

import com.manacommunity.api.serviceplatform.entity.ProviderServiceOffering;
import com.manacommunity.api.serviceplatform.entity.enums.ServiceUrgency;
import com.manacommunity.api.serviceplatform.pricing.dto.PriceCalculationRequest;
import com.manacommunity.api.serviceplatform.pricing.dto.PriceCalculationResponse;
import com.manacommunity.api.serviceplatform.pricing.entity.ServiceCoupon;
import com.manacommunity.api.serviceplatform.pricing.entity.ServicePricingRule;
import com.manacommunity.api.serviceplatform.pricing.repository.ServiceCouponRepository;
import com.manacommunity.api.serviceplatform.pricing.repository.ServicePricingRuleRepository;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PricingEngine {

    private final ServicePricingRuleRepository pricingRuleRepository;
    private final ServiceCouponRepository couponRepository;
    private final ProviderServiceOfferingRepository offeringRepository;

    @Transactional(readOnly = true)
    public PriceCalculationResponse calculatePrice(PriceCalculationRequest request) {
        BigDecimal base = request.getBaseAmount();
        if (base == null && request.getOfferingId() != null) {
            ProviderServiceOffering offering = offeringRepository.findById(request.getOfferingId()).orElse(null);
            if (offering != null) {
                base = offering.getBasePrice();
            }
        }
        if (base == null) base = BigDecimal.ZERO;

        BigDecimal surcharge = BigDecimal.ZERO;
        BigDecimal multiplier = BigDecimal.ONE;

        if (request.getUrgency() != null && request.getUrgency() != ServiceUrgency.NORMAL) {
            ServicePricingRule rule = null;
            if (request.getCategoryId() != null) {
                rule = pricingRuleRepository.findByCategoryIdAndUrgencyAndIsActiveTrue(request.getCategoryId(), request.getUrgency())
                        .orElse(null);
            }
            if (rule == null) {
                rule = pricingRuleRepository.findByUrgencyAndIsActiveTrue(request.getUrgency()).orElse(null);
            }

            if (rule != null) {
                if (rule.getMultiplier() != null && rule.getMultiplier().compareTo(BigDecimal.ONE) > 0) {
                    multiplier = rule.getMultiplier();
                }
                if (rule.getFlatSurcharge() != null) {
                    surcharge = rule.getFlatSurcharge();
                }
            } else if (request.getUrgency() == ServiceUrgency.EMERGENCY) {
                multiplier = new BigDecimal("1.50");
            } else if (request.getUrgency() == ServiceUrgency.URGENT) {
                multiplier = new BigDecimal("1.20");
            }
        }

        BigDecimal priceBeforeDiscount = base.multiply(multiplier).add(surcharge).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = BigDecimal.ZERO;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            ServiceCoupon coupon = couponRepository.findByCodeAndIsActiveTrue(request.getCouponCode().trim().toUpperCase()).orElse(null);
            if (coupon != null && !LocalDate.now().isBefore(coupon.getValidFrom()) && !LocalDate.now().isAfter(coupon.getValidTo())) {
                if (coupon.getMinOrderValue() == null || priceBeforeDiscount.compareTo(coupon.getMinOrderValue()) >= 0) {
                    if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
                        discount = priceBeforeDiscount.multiply(coupon.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    } else {
                        discount = coupon.getDiscountValue();
                    }
                    if (coupon.getMaxDiscountCap() != null && discount.compareTo(coupon.getMaxDiscountCap()) > 0) {
                        discount = coupon.getMaxDiscountCap();
                    }
                }
            }
        }

        BigDecimal finalPrice = priceBeforeDiscount.subtract(discount).max(BigDecimal.ZERO);

        return PriceCalculationResponse.builder()
                .basePrice(base)
                .surcharge(priceBeforeDiscount.subtract(base))
                .discountAmount(discount)
                .finalPrice(finalPrice)
                .breakdown("Base: " + base + ", Multiplier: " + multiplier + ", Surcharge: " + surcharge + ", Discount: " + discount)
                .build();
    }
}
