package com.manacommunity.api.commerce.core.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for creating a recurring commerce subscription.
 *
 * <p>The recurring price is resolved from the product catalogue server-side from
 * {@link #productId} / {@link #sku}. {@link #unitPrice} is advisory and ignored.
 */
@Data
public class CommerceSubscriptionRequest {
    /** Catalogue product id; used to price the subscription when {@link #sku} is absent. */
    private Long productId;
    /** Advisory only — the stored name comes from the catalogue product. */
    private String productName;
    /** Catalogue SKU; the primary key used to price the subscription. */
    private String sku;
    private Long sellerId;
    private String sellerName;
    /**
     * ADVISORY / IGNORED — do not wire this back into pricing.
     *
     * <p>The recurring price comes from the catalogue ({@code discountPrice ?? basePrice}),
     * the same rule as the one-off checkout. A client-set price here would fix what the
     * resident is charged for every future delivery, so it is only logged at WARN when it
     * disagrees with the catalogue.
     */
    private BigDecimal unitPrice;
    private Integer quantity;
    private String channel;
    private String frequency;   // DAILY WEEKLY FORTNIGHTLY MONTHLY
    private String startDate;   // ISO date yyyy-MM-dd (optional, defaults to today)
    private Integer totalCycles; // null = infinite
    private String deliveryAddress;
    private String notes;
}
