package com.manacommunity.api.payments.config.dto;

import com.manacommunity.api.payments.config.entity.PlatformPaymentConfig;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Public DTO for platform payment config — omits internal fields (updatedBy, raw entity id).
 */
@Data
@Builder
public class PlatformPaymentConfigResponse {

    private boolean cashfreeEnabled;
    private boolean razorpayEnabled;
    private boolean manaPayEnabled;
    private boolean bbpsEnabled;
    private String defaultGateway;
    private BigDecimal convenienceFeePercent;
    private BigDecimal maxTransactionAmount;
    private OffsetDateTime updatedAt;

    public static PlatformPaymentConfigResponse from(PlatformPaymentConfig entity) {
        return PlatformPaymentConfigResponse.builder()
                .cashfreeEnabled(entity.isCashfreeEnabled())
                .razorpayEnabled(entity.isRazorpayEnabled())
                .manaPayEnabled(entity.isManaPayEnabled())
                .bbpsEnabled(entity.isBbpsEnabled())
                .defaultGateway(entity.getDefaultGateway())
                .convenienceFeePercent(entity.getConvenienceFeePercent())
                .maxTransactionAmount(entity.getMaxTransactionAmount())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
