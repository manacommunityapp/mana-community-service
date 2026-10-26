package com.manacommunity.api.insurance.partner;

import com.manacommunity.api.insurance.entity.InsuranceEnums;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Development-only adapter. Enabled solely by {@code insurance.sandbox.enabled=true}. Values are fixed,
 * clearly synthetic, and every quote derived from it is persisted with source SANDBOX and rendered with a
 * "SAMPLE – not a real quote" label. It is never wired in production profiles.
 */
@Component
@ConditionalOnProperty(name = "insurance.sandbox.enabled", havingValue = "true")
public class SandboxPartnerAdapter implements PartnerAdapter {

    public static final String CODE = "SANDBOX";

    @Override public String partnerCode() { return CODE; }

    @Override public boolean isSandbox() { return true; }

    @Override
    public QuoteOffer requestQuote(QuoteInput input) {
        BigDecimal base = switch (input.category()) {
            case HEALTH, FAMILY_FLOATER, PARENTS -> new BigDecimal("12000");
            case LIFE_TERM -> new BigDecimal("9000");
            case MOTOR_CAR -> new BigDecimal("7000");
            case MOTOR_TWO_WHEELER -> new BigDecimal("1800");
            case HOME -> new BigDecimal("3000");
            case TRAVEL -> new BigDecimal("900");
            default -> new BigDecimal("2000");
        };
        return new QuoteOffer("SBX-" + input.idempotencyKey(), base, "ANNUAL",
                input.category() == InsuranceEnums.Category.TRAVEL ? new BigDecimal("500000") : new BigDecimal("1000000"),
                LocalDateTime.now().plusMinutes(30));
    }
}
