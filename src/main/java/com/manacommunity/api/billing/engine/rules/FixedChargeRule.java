package com.manacommunity.api.billing.engine.rules;

import com.manacommunity.api.billing.engine.FeeContext;
import com.manacommunity.api.billing.engine.FeeRule;

import java.math.BigDecimal;
import java.util.Set;

public class FixedChargeRule implements FeeRule {

    private final String id;
    private final String desc;
    private final BigDecimal amount;
    private final Set<String> applicableFlatTypes;

    public FixedChargeRule(String id, String desc, BigDecimal amount, Set<String> applicableFlatTypes) {
        this.id = id;
        this.desc = desc;
        this.amount = amount;
        this.applicableFlatTypes = applicableFlatTypes;
    }

    @Override
    public String ruleId() { return id; }

    @Override
    public String description() { return desc; }

    @Override
    public int priority() { return 20; }

    @Override
    public boolean appliesTo(FeeContext context) {
        return applicableFlatTypes == null || applicableFlatTypes.isEmpty()
                || applicableFlatTypes.contains(context.flatType());
    }

    @Override
    public BigDecimal calculate(FeeContext context) { return amount; }
}
