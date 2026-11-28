package com.manacommunity.api.billing.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class FeeContext {

    private final Long communityId;
    private final Long residentId;
    private final String flatNo;
    private final BigDecimal flatAreaSqft;
    private final String flatType;
    private final LocalDate billingDate;
    private final int overdueMonths;
    private final BigDecimal outstandingAmount;
    private final boolean gstExempt;
    private final boolean hasActiveWaiver;
    private final Map<String, Object> attributes = new HashMap<>();

    public FeeContext(Long communityId, Long residentId, String flatNo, BigDecimal flatAreaSqft,
                      String flatType, LocalDate billingDate, int overdueMonths,
                      BigDecimal outstandingAmount, boolean gstExempt, boolean hasActiveWaiver) {
        this.communityId = communityId;
        this.residentId = residentId;
        this.flatNo = flatNo;
        this.flatAreaSqft = flatAreaSqft;
        this.flatType = flatType;
        this.billingDate = billingDate;
        this.overdueMonths = overdueMonths;
        this.outstandingAmount = outstandingAmount;
        this.gstExempt = gstExempt;
        this.hasActiveWaiver = hasActiveWaiver;
    }

    public Long communityId() { return communityId; }
    public Long residentId() { return residentId; }
    public String flatNo() { return flatNo; }
    public BigDecimal flatAreaSqft() { return flatAreaSqft; }
    public String flatType() { return flatType; }
    public LocalDate billingDate() { return billingDate; }
    public int overdueMonths() { return overdueMonths; }
    public BigDecimal outstandingAmount() { return outstandingAmount; }
    public boolean gstExempt() { return gstExempt; }
    public boolean hasActiveWaiver() { return hasActiveWaiver; }

    public void setAttribute(String key, Object value) { attributes.put(key, value); }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }
}
