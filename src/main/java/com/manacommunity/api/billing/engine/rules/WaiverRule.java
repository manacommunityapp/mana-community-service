package com.manacommunity.api.billing.engine.rules;

import com.manacommunity.api.billing.engine.FeeContext;
import com.manacommunity.api.billing.engine.FeeRule;

import java.math.BigDecimal;

public class WaiverRule implements FeeRule {

    @Override
    public String ruleId() { return "WAIVER_DISCOUNT"; }

    @Override
    public String description() { return "Approved waiver — zeroes out penalty charges"; }

    @Override
    public int priority() { return 90; }

    @Override
    public boolean appliesTo(FeeContext context) { return context.hasActiveWaiver(); }

    @Override
    public BigDecimal calculate(FeeContext context) { return BigDecimal.ZERO; }
}
