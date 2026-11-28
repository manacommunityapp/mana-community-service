package com.manacommunity.api.architecture.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
public class BuildVsBuyAnalysisService {

    public BuildVsBuyDecision analyze(long dailyCalls, double avgCallDurationSec,
                                       int engineeringTeamSize, boolean traiDltRequired,
                                       int targetLatencyMs) {
        BigDecimal monthlyCallsM = BigDecimal.valueOf(dailyCalls * 30).divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.HALF_UP);

        // CPaaS costs (Twilio/Exotel pricing model)
        BigDecimal cpaasPerMinRate = new BigDecimal("0.80"); // ₹0.80/min India rates
        BigDecimal monthlyMinutes = BigDecimal.valueOf(dailyCalls * 30).multiply(BigDecimal.valueOf(avgCallDurationSec / 60.0));
        BigDecimal cpaasMonthlyCost = monthlyMinutes.multiply(cpaasPerMinRate);
        BigDecimal cpaasAnnualCost = cpaasMonthlyCost.multiply(BigDecimal.valueOf(12));
        BigDecimal cpaasSetupCost = new BigDecimal("500000"); // integration effort

        // In-house WebRTC costs
        BigDecimal mediaServerCostPerMonth = calculateMediaServerCost(dailyCalls);
        BigDecimal stunTurnCostPerMonth = BigDecimal.valueOf(dailyCalls > 500000 ? 150000 : 50000);
        BigDecimal inhouseMonthlyCost = mediaServerCostPerMonth.add(stunTurnCostPerMonth);
        BigDecimal inhouseAnnualCost = inhouseMonthlyCost.multiply(BigDecimal.valueOf(12));
        int requiredEngineers = dailyCalls > 1_000_000 ? 5 : 3;
        BigDecimal engineeringCostAnnual = BigDecimal.valueOf(requiredEngineers * 2400000L); // ₹24L/yr per engineer
        BigDecimal inhouseTotalYear1 = inhouseAnnualCost.add(engineeringCostAnnual).add(new BigDecimal("3000000")); // 30L setup
        BigDecimal inhouseTotalYear2Plus = inhouseAnnualCost.add(engineeringCostAnnual.multiply(new BigDecimal("0.5"))); // maintenance mode

        // Break-even analysis
        BigDecimal annualSavings = cpaasAnnualCost.subtract(inhouseTotalYear2Plus);
        int breakEvenMonths = annualSavings.compareTo(BigDecimal.ZERO) > 0
                ? inhouseTotalYear1.subtract(cpaasAnnualCost).divide(annualSavings.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP), 0, RoundingMode.CEILING).intValue()
                : -1;

        // Decision matrix
        List<Dimension> dimensions = new ArrayList<>();
        dimensions.add(scoreDimension("Unit Economics", 30,
                dailyCalls > 1_000_000 ? 8 : 4, dailyCalls > 1_000_000 ? 4 : 8,
                "At " + monthlyCallsM + "M calls/month, " + (dailyCalls > 1_000_000 ? "in-house becomes cheaper after break-even" : "CPaaS volume discounts are more cost-effective")));
        dimensions.add(scoreDimension("Time to Market", 20, 3, 9,
                "CPaaS: 2-4 weeks integration. In-house: 6-9 months to production-ready WebRTC stack"));
        dimensions.add(scoreDimension("TRAI DLT Compliance", 15,
                traiDltRequired ? 7 : 5, traiDltRequired ? 6 : 8,
                traiDltRequired ? "Both need DLT registration; in-house gives header/template control" : "Standard compliance — CPaaS handles most regulatory burden"));
        dimensions.add(scoreDimension("Latency Control", 15,
                targetLatencyMs < 200 ? 9 : 6, targetLatencyMs < 200 ? 5 : 7,
                targetLatencyMs < 200 ? "Sub-200ms needs edge servers; in-house allows India-specific PoP placement" : "Standard latency achievable with CPaaS regional endpoints"));
        dimensions.add(scoreDimension("Operational Complexity", 10, 3, 9,
                "In-house: own SRTP, SRTP-to-PSTN gateway, certificate rotation, oCall monitoring. CPaaS: managed"));
        dimensions.add(scoreDimension("Vendor Lock-in Risk", 10, 9, 3,
                "In-house: full control. CPaaS: API lock-in, pricing changes, deprecation risk"));

        int buildScore = dimensions.stream().mapToInt(d -> d.buildScore * d.weight / 100).sum();
        int buyScore = dimensions.stream().mapToInt(d -> d.buyScore * d.weight / 100).sum();
        String recommendation = buildScore > buyScore ? "BUILD" : "BUY";

        // Hybrid option
        String hybridStrategy = "Use CPaaS for PSTN/IVR (guard-to-resident calls) + in-house WebRTC for app-to-app video intercom. " +
                "Migrate PSTN to in-house SIP trunk at " + (dailyCalls > 500000 ? "current scale" : "1M+ daily calls") + " when unit economics flip.";

        return new BuildVsBuyDecision(
                recommendation, buildScore, buyScore, dimensions,
                new CostBreakdown("CPaaS (Twilio/Exotel)", cpaasMonthlyCost, cpaasAnnualCost, cpaasSetupCost, 0),
                new CostBreakdown("In-House WebRTC", inhouseMonthlyCost, inhouseTotalYear1, new BigDecimal("3000000"), requiredEngineers),
                breakEvenMonths > 0 ? breakEvenMonths + " months" : "Never (CPaaS remains cheaper)",
                hybridStrategy,
                Map.of("dailyCalls", dailyCalls, "monthlyCallsM", monthlyCallsM,
                        "avgDurationSec", avgCallDurationSec, "traiDlt", traiDltRequired,
                        "teamSize", engineeringTeamSize, "targetLatencyMs", targetLatencyMs));
    }

    private BigDecimal calculateMediaServerCost(long dailyCalls) {
        if (dailyCalls > 2_000_000) return new BigDecimal("800000");
        if (dailyCalls > 1_000_000) return new BigDecimal("500000");
        if (dailyCalls > 500_000) return new BigDecimal("250000");
        return new BigDecimal("100000");
    }

    private Dimension scoreDimension(String name, int weight, int buildScore, int buyScore, String rationale) {
        return new Dimension(name, weight, buildScore, buyScore, rationale);
    }

    public record BuildVsBuyDecision(String recommendation, int buildTotalScore, int buyTotalScore,
                                      List<Dimension> dimensions, CostBreakdown cpaasCost,
                                      CostBreakdown inhouseCost, String breakEvenPoint,
                                      String hybridStrategy, Map<String, Object> context) {}

    public record Dimension(String name, int weight, int buildScore, int buyScore, String rationale) {}

    public record CostBreakdown(String option, BigDecimal monthlyCost, BigDecimal annualCost,
                                 BigDecimal setupCost, int requiredEngineers) {}
}
