package com.manacommunity.api.opsresilience.service;

import com.manacommunity.api.opsresilience.entity.TenantAccountingRule;
import com.manacommunity.api.opsresilience.repository.TenantAccountingRuleRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Config-Over-Code Declarative Tenant Accounting Rule Service:
 * Allows township-specific billing compromises (penalty waivers, GST overrides, sinking-fund surcharges, late fee caps)
 * without custom per-tenant branches in core accounting services. Supports dry-run previews before live enforcement.
 */
@Service
@RequiredArgsConstructor
public class TenantAccountingConfigService {

    private static final Set<String> ALLOWED_RULE_TYPES = Set.of(
            "PENALTY_WAIVER", "GST_OVERRIDE", "SINKING_FUND_SURCHARGE", "LATE_FEE_CAP"
    );

    private final TenantAccountingRuleRepository ruleRepo;

    @Builder
    public record UpsertRuleRequest(
            Long communityId,
            String ruleKey,
            String ruleType,
            int rateBps,
            long flatAmountPaise,
            Instant effectiveFrom,
            Instant effectiveTo,
            boolean dryRunOnly,
            boolean active,
            String auditReason
    ) {}

    @Builder
    public record RuleEvaluationResult(
            long originalPaise,
            long previewAdjustedPaise,
            long effectiveCommittedPaise,
            long deltaPaise,
            String appliedRuleKey,
            boolean dryRunOnly,
            boolean ruleApplied
    ) {}

    @Transactional
    public TenantAccountingRule upsertRule(UpsertRuleRequest req) {
        String type = req.ruleType() != null ? req.ruleType().trim().toUpperCase(Locale.ROOT) : "";
        if (!ALLOWED_RULE_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported accounting rule type: " + req.ruleType());
        }
        if (req.rateBps() < 0 || req.rateBps() > 10_000) {
            throw new IllegalArgumentException("rateBps must be between 0 and 10000");
        }
        if (req.flatAmountPaise() < 0) {
            throw new IllegalArgumentException("flatAmountPaise cannot be negative");
        }
        if (req.effectiveFrom() == null || req.effectiveTo() == null || !req.effectiveTo().isAfter(req.effectiveFrom())) {
            throw new IllegalArgumentException("effectiveTo must be strictly after effectiveFrom");
        }

        String normKey = req.ruleKey().trim().toUpperCase(Locale.ROOT);
        TenantAccountingRule rule = ruleRepo.findByCommunityIdAndRuleKey(req.communityId(), normKey)
                .orElseGet(() -> TenantAccountingRule.builder()
                        .communityId(req.communityId())
                        .ruleKey(normKey)
                        .build());
        rule.setRuleType(type);
        rule.setRateBps(req.rateBps());
        rule.setFlatAmountPaise(req.flatAmountPaise());
        rule.setEffectiveFrom(req.effectiveFrom());
        rule.setEffectiveTo(req.effectiveTo());
        rule.setDryRunOnly(req.dryRunOnly());
        rule.setActive(req.active());
        rule.setAuditReason(req.auditReason());
        return ruleRepo.save(rule);
    }

    @Transactional(readOnly = true)
    public RuleEvaluationResult evaluateRule(Long communityId, String ruleKey, long basePaise, Instant atTime) {
        Instant evalAt = atTime != null ? atTime : Instant.now();
        String normKey = ruleKey.trim().toUpperCase(Locale.ROOT);
        TenantAccountingRule rule = ruleRepo.findByCommunityIdAndRuleKey(communityId, normKey).orElse(null);

        if (rule == null || !rule.isActive()
                || evalAt.isBefore(rule.getEffectiveFrom())
                || evalAt.isAfter(rule.getEffectiveTo())) {
            return RuleEvaluationResult.builder()
                    .originalPaise(basePaise)
                    .previewAdjustedPaise(basePaise)
                    .effectiveCommittedPaise(basePaise)
                    .deltaPaise(0L)
                    .appliedRuleKey(normKey)
                    .dryRunOnly(false)
                    .ruleApplied(false)
                    .build();
        }

        long delta = switch (rule.getRuleType()) {
            case "PENALTY_WAIVER" -> -Math.min(basePaise, (basePaise * rule.getRateBps()) / 10_000L + rule.getFlatAmountPaise());
            case "SINKING_FUND_SURCHARGE", "GST_OVERRIDE" -> (basePaise * rule.getRateBps()) / 10_000L + rule.getFlatAmountPaise();
            case "LATE_FEE_CAP" -> {
                long cap = rule.getFlatAmountPaise();
                yield basePaise > cap ? (cap - basePaise) : 0L;
            }
            default -> 0L;
        };

        long preview = Math.max(0L, basePaise + delta);
        long committed = rule.isDryRunOnly() ? basePaise : preview;

        return RuleEvaluationResult.builder()
                .originalPaise(basePaise)
                .previewAdjustedPaise(preview)
                .effectiveCommittedPaise(committed)
                .deltaPaise(preview - basePaise)
                .appliedRuleKey(rule.getRuleKey())
                .dryRunOnly(rule.isDryRunOnly())
                .ruleApplied(true)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TenantAccountingRule> listActiveRules(Long communityId) {
        return ruleRepo.findByCommunityIdAndActiveTrueOrderByCreatedAtDesc(communityId);
    }
}
