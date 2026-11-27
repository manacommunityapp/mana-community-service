package com.manacommunity.api.billing.engine.rules;

import com.manacommunity.api.billing.engine.FeeContext;
import com.manacommunity.api.billing.engine.FeeRule;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GstRule implements FeeRule {

    private final BigDecimal cgstRate;
    private final BigDecimal sgstRate;

    public GstRule(BigDecimal cgstRate, BigDecimal sgstRate) {
        this.cgstRate = cgstRate;
        this.sgstRate = sgstRate;
    }

    @Override
    public String ruleId() { return "GST_18"; }

    @Override
    public String description() { return "GST (CGST " + pct(cgstRate) + "% + SGST " + pct(sgstRate) + "%)"; }

    @Override
    public int priority() { return 100; }

    @Override
    public boolean appliesTo(FeeContext context) {
        return !context.gstExempt();
    }

    @Override
    public BigDecimal calculate(FeeContext context) {
        BigDecimal subtotal = context.getAttribute("subtotal");
        if (subtotal == null) return BigDecimal.ZERO;
        BigDecimal totalRate = cgstRate.add(sgstRate);
        return subtotal.multiply(totalRate).setScale(2, RoundingMode.HALF_UP);
    }

    private String pct(BigDecimal rate) {
        return rate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString();
    }
}
