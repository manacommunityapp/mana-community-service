package com.manacommunity.api.automation.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class AutomationRuleEngine {

    public enum TriggerDomain {
        GROUP_BUY, FINANCE, HELPDESK, VISITOR_SECURITY, SMART_METER, PARKING
    }

    public enum ComparisonOperator {
        GTE, LTE, GT, LT, EQ, EQUALS_BOOLEAN
    }

    public record RuleEvaluationResult(
            boolean isTriggered,
            String ruleName,
            String triggeredAction,
            String reason,
            boolean isDelayed
    ) {}

    public RuleEvaluationResult evaluate(
            String ruleName,
            String conditionKey,
            ComparisonOperator operator,
            Object thresholdValue,
            String actionType,
            Map<String, Object> eventPayload
    ) {
        if (eventPayload == null || !eventPayload.containsKey(conditionKey)) {
            return new RuleEvaluationResult(false, ruleName, actionType, "Missing event key: " + conditionKey, false);
        }

        Object actualVal = eventPayload.get(conditionKey);
        boolean matched = matches(operator, actualVal, thresholdValue);

        if (matched) {
            return new RuleEvaluationResult(
                    true,
                    ruleName,
                    actionType,
                    "Condition satisfied: " + conditionKey + " (" + actualVal + ") " + operator + " " + thresholdValue,
                    false
            );
        }

        return new RuleEvaluationResult(
                false,
                ruleName,
                actionType,
                "Condition not satisfied: " + conditionKey + " (" + actualVal + ")",
                false
        );
    }

    /**
     * Evaluates a delayed workflow where initial condition triggers delay, and recheck condition is evaluated after delay.
     */
    public RuleEvaluationResult evaluateDelayedWorkflow(
            String ruleName,
            String initialConditionKey,
            ComparisonOperator initialOperator,
            Object initialThreshold,
            String recheckConditionKey,
            ComparisonOperator recheckOperator,
            Object recheckThreshold,
            String actionType,
            Map<String, Object> initialPayload,
            Map<String, Object> stateAfterDelay
    ) {
        // 1. Evaluate Initial Event
        if (initialPayload == null || !initialPayload.containsKey(initialConditionKey)) {
            return new RuleEvaluationResult(false, ruleName, actionType, "Initial event missing key: " + initialConditionKey, true);
        }
        boolean initialMatched = matches(initialOperator, initialPayload.get(initialConditionKey), initialThreshold);
        if (!initialMatched) {
            return new RuleEvaluationResult(false, ruleName, actionType, "Initial event did not trigger delay window", true);
        }

        // 2. Evaluate State After Delay
        if (stateAfterDelay == null || !stateAfterDelay.containsKey(recheckConditionKey)) {
            return new RuleEvaluationResult(false, ruleName, actionType, "Post-delay state missing recheck key: " + recheckConditionKey, true);
        }
        boolean postDelayMatched = matches(recheckOperator, stateAfterDelay.get(recheckConditionKey), recheckThreshold);

        if (postDelayMatched) {
            return new RuleEvaluationResult(
                    true,
                    ruleName,
                    actionType,
                    "Delayed workflow executed: condition persisted after wait duration.",
                    true
            );
        }

        return new RuleEvaluationResult(
                false,
                ruleName,
                actionType,
                "Delayed workflow cancelled early: condition cleared during wait duration.",
                true
        );
    }

    private boolean matches(ComparisonOperator operator, Object actualVal, Object thresholdValue) {
        if (operator == ComparisonOperator.EQUALS_BOOLEAN) {
            boolean expected = Boolean.parseBoolean(String.valueOf(thresholdValue));
            boolean actual = Boolean.parseBoolean(String.valueOf(actualVal));
            return (expected == actual);
        } else {
            BigDecimal actualNum = new BigDecimal(String.valueOf(actualVal));
            BigDecimal thresholdNum = new BigDecimal(String.valueOf(thresholdValue));
            int cmp = actualNum.compareTo(thresholdNum);

            return switch (operator) {
                case GTE -> cmp >= 0;
                case LTE -> cmp <= 0;
                case GT  -> cmp > 0;
                case LT  -> cmp < 0;
                case EQ  -> cmp == 0;
                default  -> false;
            };
        }
    }
}