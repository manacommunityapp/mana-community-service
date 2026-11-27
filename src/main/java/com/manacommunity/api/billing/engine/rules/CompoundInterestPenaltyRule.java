package com.manacommunity.api.billing.engine.rules;

import com.manacommunity.api.billing.engine.FeeContext;
import com.manacommunity.api.billing.engine.FeeRule;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class CompoundInterestPenaltyRule implements FeeRule {

    private final BigDecimal monthlyRate;
    private final boolean compound;

    public CompoundInterestPenaltyRule(BigDecimal monthlyRate, boolean compound) {
        this.monthlyRate = monthlyRate;
        this.compound = compound;
    }

    @Override
    public String ruleId() { return compound ? "COMPOUND_LATE_INTEREST" : "SIMPLE_LATE_INTEREST"; }

    @Override
    public String description() {
        return (compound ? "Compound" : "Simple") + " late-payment interest at " + monthlyRate.multiply(new BigDecimal("100")) + "% per month";
    }

    @Override
    public int priority() { return 50; }

    @Override
    public boolean appliesTo(FeeContext context) {
        return !context.hasActiveWaiver()
                && context.overdueMonths() > 0
                && context.outstandingAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public BigDecimal calculate(FeeContext context) {
        BigDecimal principal = context.outstandingAmount();
        int months = context.overdueMonths();

        if (compound) {
            BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months, MathContext.DECIMAL128);
            return principal.multiply(factor).subtract(principal).setScale(2, RoundingMode.HALF_UP);
        } else {
            return principal.multiply(monthlyRate).multiply(new BigDecimal(months)).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
