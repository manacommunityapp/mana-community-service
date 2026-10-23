package com.manacommunity.api.serviceplatform.pricing.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.serviceplatform.entity.ServiceProvider;
import com.manacommunity.api.serviceplatform.entity.ServiceRequest;
import com.manacommunity.api.serviceplatform.pricing.dto.PriceCalculationRequest;
import com.manacommunity.api.serviceplatform.pricing.dto.PriceCalculationResponse;
import com.manacommunity.api.serviceplatform.pricing.engine.PricingEngine;
import com.manacommunity.api.serviceplatform.pricing.entity.ServiceCoupon;
import com.manacommunity.api.serviceplatform.pricing.entity.ServiceQuote;
import com.manacommunity.api.serviceplatform.pricing.repository.ServiceCouponRepository;
import com.manacommunity.api.serviceplatform.pricing.repository.ServicePricingRuleRepository;
import com.manacommunity.api.serviceplatform.pricing.repository.ServiceQuoteRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceProviderRepository;
import com.manacommunity.api.serviceplatform.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final PricingEngine pricingEngine;
    private final ServicePricingRuleRepository ruleRepository;
    private final ServiceCouponRepository couponRepository;
    private final ServiceQuoteRepository quoteRepository;
    private final ServiceRequestRepository requestRepository;
    private final ServiceProviderRepository providerRepository;

    @Transactional(readOnly = true)
    public PriceCalculationResponse estimatePrice(PriceCalculationRequest request) {
        return pricingEngine.calculatePrice(request);
    }

    @Transactional
    public ServiceQuote submitQuote(Long serviceRequestId, Long providerId, BigDecimal amount, Integer duration, String notes) {
        ServiceRequest req = requestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", serviceRequestId));
        ServiceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceProvider", providerId));

        ServiceQuote quote = ServiceQuote.builder()
                .serviceRequest(req)
                .provider(provider)
                .quotedAmount(amount)
                .estimatedDurationMinutes(duration)
                .notes(notes)
                .status("SUBMITTED")
                .build();

        return quoteRepository.save(quote);
    }

    @Transactional(readOnly = true)
    public List<ServiceQuote> getQuotesForRequest(Long requestId) {
        return quoteRepository.findByServiceRequestId(requestId);
    }

    @Transactional
    public ServiceCoupon saveCoupon(ServiceCoupon coupon) {
        return couponRepository.save(coupon);
    }
}
