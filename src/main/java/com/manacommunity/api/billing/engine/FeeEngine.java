package com.manacommunity.api.billing.engine;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class FeeEngine {

    private final List<FeeRule> rules = new CopyOnWriteArrayList<>();

    public void registerRule(FeeRule rule) {
        rules.add(rule);
        rules.sort(Comparator.comparingInt(FeeRule::priority));
        log.info("Registered fee rule: {} (priority {})", rule.ruleId(), rule.priority());
    }

    public void removeRule(String ruleId) {
        rules.removeIf(r -> r.ruleId().equals(ruleId));
    }

    public InvoiceBreakdown calculate(FeeContext context) {
        List<LineItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal penaltyTotal = BigDecimal.ZERO;
        BigDecimal gstTotal = BigDecimal.ZERO;

        for (FeeRule rule : rules) {
            if (!rule.appliesTo(context)) continue;

            if (rule.priority() >= 100) {
                context.setAttribute("subtotal", subtotal.add(penaltyTotal));
            }

            BigDecimal amount = rule.calculate(context);

            if (amount.compareTo(BigDecimal.ZERO) == 0 && rule.priority() >= 90) {
                continue;
            }

            items.add(new LineItem(rule.ruleId(), rule.description(), amount, rule.priority()));

            if (rule.priority() >= 100) {
                gstTotal = gstTotal.add(amount);
            } else if (rule.priority() >= 50) {
                penaltyTotal = penaltyTotal.add(amount);
            } else {
                subtotal = subtotal.add(amount);
            }
        }

        BigDecimal grandTotal = subtotal.add(penaltyTotal).add(gstTotal);
        return new InvoiceBreakdown(items, subtotal, penaltyTotal, gstTotal, grandTotal);
    }

    public List<RuleInfo> listRules() {
        return rules.stream()
                .map(r -> new RuleInfo(r.ruleId(), r.description(), r.priority()))
                .toList();
    }

    public record LineItem(String ruleId, String description, BigDecimal amount, int priority) {}
    public record InvoiceBreakdown(List<LineItem> lineItems, BigDecimal subtotal,
                                    BigDecimal penalties, BigDecimal gst, BigDecimal grandTotal) {}
    public record RuleInfo(String ruleId, String description, int priority) {}
}
