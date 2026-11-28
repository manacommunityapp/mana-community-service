package com.manacommunity.api.billing.engine;

import java.math.BigDecimal;

public interface FeeRule {

    String ruleId();

    String description();

    int priority();

    boolean appliesTo(FeeContext context);

    BigDecimal calculate(FeeContext context);
}
