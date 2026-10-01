package com.manacommunity.api.serviceplatform.unit;

import com.manacommunity.api.serviceplatform.entity.enums.ServiceUrgency;
import com.manacommunity.api.serviceplatform.pricing.dto.PriceCalculationRequest;
import com.manacommunity.api.serviceplatform.pricing.dto.PriceCalculationResponse;
import com.manacommunity.api.serviceplatform.pricing.engine.PricingEngine;
import com.manacommunity.api.serviceplatform.pricing.entity.PricingRule;
import com.manacommunity.api.serviceplatform.pricing.entity.ServiceCoupon;
import com.manacommunity.api.serviceplatform.pricing.repository.PricingRuleRepository;
import com.manacommunity.api.serviceplatform.pricing.repository.ServiceCouponRepository;
import com.manacommunity.api.serviceplatform.repository.ProviderServiceOfferingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pricing Engine Unit Tests")
class PricingEngineTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private ServiceCouponRepository couponRepository;

    @Mock
    private ProviderServiceOfferingRepository offeringRepository;

    @InjectMocks
    private PricingEngine engine;

    @Test
    @DisplayName("Should calculate base price correctly for normal urgency")
    void testCalculateBasePriceNormal() {
        PriceCalculationRequest req = PriceCalculationRequest.builder()
                .baseAmount(new BigDecimal("1000.00"))
                .urgency(ServiceUrgency.NORMAL)
                .build();

        PriceCalculationResponse res = engine.calculatePrice(req);

        assertNotNull(res);
        assertEquals(0, new BigDecimal("1000.00").compareTo(res.getBasePrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(res.getSurcharge()));
        assertEquals(0, BigDecimal.ZERO.compareTo(res.getDiscountAmount()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(res.getFinalPrice()));
    }

    @Test
    @DisplayName("Should apply emergency multiplier and coupon discount")
    void testCalculateWithEmergencyAndCoupon() {
        PriceCalculationRequest req = PriceCalculationRequest.builder()
                .baseAmount(new BigDecimal("1000.00"))
                .urgency(ServiceUrgency.EMERGENCY)
                .couponCode("SAVE10")
                .build();

        PricingRule emergencyRule = PricingRule.builder()
                .urgency(ServiceUrgency.EMERGENCY)
                .multiplier(new BigDecimal("1.50"))
                .isActive(true)
                .build();

        ServiceCoupon coupon = ServiceCoupon.builder()
                .code("SAVE10")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("10.00"))
                .validFrom(LocalDate.now().minusDays(1))
                .validTo(LocalDate.now().plusDays(10))
                .isActive(true)
                .build();

        when(pricingRuleRepository.findByUrgencyAndIsActiveTrue(ServiceUrgency.EMERGENCY))
                .thenReturn(Optional.of(emergencyRule));
        when(couponRepository.findByCodeAndIsActiveTrue("SAVE10"))
                .thenReturn(Optional.of(coupon));

        PriceCalculationResponse res = engine.calculatePrice(req);

        assertNotNull(res);
        assertEquals(0, new BigDecimal("1000.00").compareTo(res.getBasePrice()));
        assertEquals(0, new BigDecimal("500.00").compareTo(res.getSurcharge()));
        assertEquals(0, new BigDecimal("150.00").compareTo(res.getDiscountAmount()));
        assertEquals(0, new BigDecimal("1350.00").compareTo(res.getFinalPrice()));
    }
}
