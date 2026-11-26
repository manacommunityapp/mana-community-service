package com.manacommunity.api.billing.engine.rules;

import com.manacommunity.api.billing.engine.FeeContext;
import com.manacommunity.api.billing.engine.FeeRule;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AreaBasedMaintenanceRule implements FeeRule {

    private final BigDecimal ratePerSqft;

    public AreaBasedMaintenanceRule(BigDecimal ratePerSqft) {
        this.ratePerSqft = ratePerSqft;
    }

    @Override
    public String ruleId() { return "AREA_BASED_MAINTENANCE"; }

    @Override
    public String description() { return "Maintenance fee based on flat area × rate per sqft"; }

    @Override
    public int priority() { return 10; }

    @Override
    public boolean appliesTo(FeeContext context) {
        return context.flatAreaSqft() != null && context.flatAreaSqft().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public BigDecimal calculate(FeeContext context) {
        return context.flatAreaSqft().multiply(ratePerSqft).setScale(2, RoundingMode.HALF_UP);
    }
}
