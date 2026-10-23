package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceCheckoutRequest;
import com.manacommunity.api.user.model.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CommerceRiskEngine {

    public record RiskResult(int score, String decision, List<String> flags) {
        public boolean isAllowed() {
            return !"BLOCKED".equalsIgnoreCase(decision);
        }
    }

    public RiskResult assessRisk(AppUser user, CommerceCheckoutRequest request) {
        int score = 0;
        List<String> flags = new ArrayList<>();

        if (user == null) {
            return new RiskResult(100, "BLOCKED", List.of("ANONYMOUS_USER"));
        }

        double totalAmount = request.getItems().stream()
                .mapToDouble(i -> (i.getUnitPrice() != null ? i.getUnitPrice().doubleValue() : 0.0) * (i.getQuantity() != null ? i.getQuantity() : 1))
                .sum();

        if (totalAmount > 50000) {
            score += 30;
            flags.add("HIGH_VALUE_TRANSACTION");
        }

        if (request.getItems().size() > 20) {
            score += 20;
            flags.add("BULK_ITEM_COUNT");
        }

        String decision = score >= 80 ? "BLOCKED" : score >= 40 ? "REVIEW_REQUIRED" : "CLEAR";
        log.info("Risk assessment for user {} (order amt: ₹{}): score={}, decision={}", user.getId(), totalAmount, score, decision);
        return new RiskResult(score, decision, flags);
    }
}
