package com.manacommunity.api.architecture.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class StranglerExtractionService {

    public ExtractionPlan generateExtractionPlan(String sourceDomain, String targetDomain,
                                                  List<String> sharedTables, int dailyTransactions,
                                                  int teamSize, boolean featureFreezeAllowed) {
        List<Phase> phases = new ArrayList<>();
        List<Risk> risks = new ArrayList<>();
        List<String> adrs = new ArrayList<>();

        // Phase 1: Identify Bounded Contexts
        phases.add(new Phase(1, "Domain Analysis & Bounded Context Mapping",
                "2 weeks", "LOW",
                List.of("Event-storm the billing domain with domain experts",
                        "Identify aggregate roots: Invoice, BillingCycle, Payment, LedgerEntry",
                        "Map shared tables: " + String.join(", ", sharedTables),
                        "Document all cross-domain queries (JOIN paths between gate_entry and billing)",
                        "Classify coupling: data coupling vs behavioral coupling vs temporal coupling"),
                Map.of("sharedTables", sharedTables.size(),
                        "estimatedJoinPaths", sharedTables.size() * 3,
                        "couplingType", "TEMPORAL_AND_DATA")));

        // Phase 2: Anti-Corruption Layer
        phases.add(new Phase(2, "Anti-Corruption Layer (ACL) Introduction",
                "3 weeks", "MEDIUM",
                List.of("Create BillingFacade interface in monolith abstracting all billing calls",
                        "Replace all direct billing repository calls with facade methods",
                        "Introduce BillingEvent domain events for async communication",
                        "Add CDC (Change Data Capture) triggers on shared tables",
                        "Implement idempotent event handlers with deduplication keys"),
                Map.of("facadeMethods", dailyTransactions > 100000 ? 25 : 15,
                        "eventTypes", 8,
                        "cdcTables", sharedTables.size())));

        // Phase 3: Database Decomposition
        String dbStrategy = dailyTransactions > 500000 ? "DUAL_WRITE_WITH_RECONCILIATION" : "CDC_WITH_OUTBOX";
        phases.add(new Phase(3, "Database Decomposition (" + dbStrategy + ")",
                "4 weeks", "HIGH",
                List.of("Create billing-specific schema with own tables (no shared PKs)",
                        "Implement " + dbStrategy + " pattern for data sync",
                        "Add reconciliation job to detect drift between schemas",
                        "Migrate billing reads to new schema first (read path strangling)",
                        "Shadow-write to both schemas, compare results for 2 weeks",
                        "Cut over write path with feature flag (billing.write.target=NEW_SCHEMA)"),
                Map.of("strategy", dbStrategy,
                        "reconciliationIntervalMinutes", 15,
                        "shadowWriteDurationWeeks", 2,
                        "rollbackTimeMinutes", 5)));

        // Phase 4: Service Extraction
        phases.add(new Phase(4, "Service Extraction & Traffic Migration",
                "3 weeks", "HIGH",
                List.of("Deploy billing service with own DB, consuming events from monolith",
                        "Strangler fig proxy: route /api/billing/* to new service via API gateway",
                        "Canary rollout: 5% → 25% → 50% → 100% over 2 weeks",
                        "Circuit breaker (Resilience4j) with fallback to monolith billing facade",
                        "Kill switch: environment variable to instantly route 100% back to monolith",
                        "Decommission billing code from monolith after 30-day bake period"),
                Map.of("canaryStages", List.of("5%", "25%", "50%", "100%"),
                        "circuitBreakerThreshold", "50% failure rate in 10s window",
                        "fallbackTarget", "MONOLITH_FACADE",
                        "bakePeriodDays", 30)));

        // Phase 5: Verification
        phases.add(new Phase(5, "Verification & Decommission",
                "2 weeks", "LOW",
                List.of("Run billing cron on extracted service, verify gate latency p99 < 50ms",
                        "Load test: simulate billing spike (3x normal) while gate traffic runs",
                        "Confirm zero cross-schema JOINs remain in monolith",
                        "Remove billing tables from monolith schema (keep 90-day archive)",
                        "Update runbooks, dashboards, and on-call routing"),
                Map.of("latencyTarget", "p99 < 50ms for gate operations during billing spike",
                        "loadTestMultiplier", 3,
                        "archiveDays", 90)));

        // Risks
        risks.add(new Risk("DATA_INCONSISTENCY", "HIGH",
                "Dual-write period may produce divergent records if one write fails",
                "Outbox pattern with transactional guarantees + reconciliation cron every 15 min"));
        risks.add(new Risk("FEATURE_VELOCITY_IMPACT", featureFreezeAllowed ? "LOW" : "HIGH",
                "Extraction work competes with feature development",
                featureFreezeAllowed ? "2-week feature freeze during Phase 3"
                        : "Branch-by-abstraction: feature work continues on facade interface"));
        risks.add(new Risk("CASCADING_FAILURE", "MEDIUM",
                "New billing service down → gate operations impacted",
                "Circuit breaker with 5s timeout, fallback to cached billing state"));
        risks.add(new Risk("TEAM_KNOWLEDGE_GAP", teamSize < 5 ? "HIGH" : "LOW",
                "Team of " + teamSize + " may lack distributed systems experience",
                "Pair programming on Phase 3-4, weekly architecture review with principal engineer"));

        // ADRs
        adrs.add("ADR-001: Use " + dbStrategy + " over direct DB migration to avoid downtime");
        adrs.add("ADR-002: Event-driven communication (Kafka/SQS) over synchronous REST for billing events");
        adrs.add("ADR-003: Strangler fig proxy at API gateway level, not in-code routing");
        adrs.add("ADR-004: Billing service owns its schema — no shared database antipattern");
        adrs.add("ADR-005: 30-day bake period before monolith code removal");

        int totalWeeks = 14;
        String estimatedEffort = totalWeeks + " weeks (" + (teamSize >= 4 ? "parallel tracks possible" : "sequential, team too small for parallelism") + ")";

        return new ExtractionPlan(sourceDomain, targetDomain, phases, risks, adrs,
                estimatedEffort, totalWeeks, teamSize,
                Map.of("dailyTransactions", dailyTransactions,
                        "sharedTables", sharedTables.size(),
                        "featureFreezeAllowed", featureFreezeAllowed));
    }

    public LatencySimulation simulateBillingImpact(int gateRequestsPerSec, int billingCronQueriesPerSec,
                                                    int sharedDbConnections, boolean extracted) {
        double baseGateLatencyMs = 12.0;
        double billingLoadFactor = (double) billingCronQueriesPerSec / sharedDbConnections;

        double gateP50, gateP99;
        if (extracted) {
            gateP50 = baseGateLatencyMs;
            gateP99 = baseGateLatencyMs * 2.5;
        } else {
            gateP50 = baseGateLatencyMs * (1 + billingLoadFactor * 0.3);
            gateP99 = baseGateLatencyMs * (1 + billingLoadFactor * 2.5);
            if (billingLoadFactor > 0.7) {
                gateP99 *= 1.8; // connection pool contention
            }
        }

        double connectionUtilization = extracted
                ? (double) gateRequestsPerSec / sharedDbConnections * 100
                : (double) (gateRequestsPerSec + billingCronQueriesPerSec) / sharedDbConnections * 100;

        return new LatencySimulation(
                extracted, gateRequestsPerSec, billingCronQueriesPerSec,
                sharedDbConnections, Math.round(gateP50 * 100.0) / 100.0,
                Math.round(gateP99 * 100.0) / 100.0,
                Math.min(connectionUtilization, 100.0),
                gateP99 > 200 ? "CRITICAL" : gateP99 > 100 ? "DEGRADED" : "HEALTHY",
                connectionUtilization > 85 ? "Pool contention likely — queries will queue" : "Within safe limits");
    }

    public record ExtractionPlan(String sourceDomain, String targetDomain, List<Phase> phases,
                                  List<Risk> risks, List<String> adrs, String estimatedEffort,
                                  int totalWeeks, int teamSize, Map<String, Object> context) {}

    public record Phase(int number, String name, String duration, String riskLevel,
                        List<String> steps, Map<String, Object> metrics) {}

    public record Risk(String id, String severity, String description, String mitigation) {}

    public record LatencySimulation(boolean extracted, int gateRps, int billingRps,
                                     int dbConnections, double gateP50Ms, double gateP99Ms,
                                     double connectionUtilizationPct, String healthStatus, String recommendation) {}
}
