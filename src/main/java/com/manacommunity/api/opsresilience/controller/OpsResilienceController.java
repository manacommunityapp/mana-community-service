package com.manacommunity.api.opsresilience.controller;

import com.manacommunity.api.opsresilience.entity.GuardExpressCheckin;
import com.manacommunity.api.opsresilience.entity.IncidentPostmortem;
import com.manacommunity.api.opsresilience.entity.PostmortemGuardrail;
import com.manacommunity.api.opsresilience.entity.TenantAccountingRule;
import com.manacommunity.api.opsresilience.service.BlamelessPostmortemService;
import com.manacommunity.api.opsresilience.service.GuardExpressCheckinService;
import com.manacommunity.api.opsresilience.service.GuardExpressCheckinService.ExpressCheckinRequest;
import com.manacommunity.api.opsresilience.service.GuardExpressCheckinService.ExpressCheckinResponse;
import com.manacommunity.api.opsresilience.service.TenantAccountingConfigService;
import com.manacommunity.api.opsresilience.service.TenantAccountingConfigService.RuleEvaluationResult;
import com.manacommunity.api.opsresilience.service.TenantAccountingConfigService.UpsertRuleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ops")
@RequiredArgsConstructor
public class OpsResilienceController {

    private final GuardExpressCheckinService guardExpressService;
    private final TenantAccountingConfigService accountingConfigService;
    private final BlamelessPostmortemService postmortemService;

    @PostMapping("/guard-express/checkin")
    public ResponseEntity<ExpressCheckinResponse> recordExpressCheckin(@RequestBody ExpressCheckinRequest req) {
        return ResponseEntity.ok(guardExpressService.recordExpressCheckin(req));
    }

    @GetMapping("/guard-express/overrides")
    public ResponseEntity<List<GuardExpressCheckin>> listPendingOverrides(@RequestParam Long communityId) {
        return ResponseEntity.ok(guardExpressService.listPendingMcOverrides(communityId));
    }

    @PostMapping("/accounting-rules")
    public ResponseEntity<TenantAccountingRule> upsertAccountingRule(@RequestBody UpsertRuleRequest req) {
        return ResponseEntity.ok(accountingConfigService.upsertRule(req));
    }

    @PostMapping("/accounting-rules/evaluate")
    public ResponseEntity<RuleEvaluationResult> evaluateAccountingRule(@RequestBody Map<String, Object> body) {
        Long communityId = Long.parseLong(body.get("communityId").toString());
        String ruleKey = body.get("ruleKey").toString();
        long basePaise = Long.parseLong(body.get("basePaise").toString());
        return ResponseEntity.ok(accountingConfigService.evaluateRule(communityId, ruleKey, basePaise, Instant.now()));
    }

    @PostMapping("/postmortems")
    public ResponseEntity<IncidentPostmortem> openPostmortem(@RequestBody Map<String, Object> body) {
        Instant startedAt = body.containsKey("startedAt") ? Instant.parse(body.get("startedAt").toString()) : Instant.now();
        return ResponseEntity.ok(postmortemService.openPostmortem(
                body.get("incidentCode").toString(),
                body.get("title").toString(),
                body.get("severity").toString(),
                Double.parseDouble(body.getOrDefault("blastRadiusPct", "0").toString()),
                body.get("rootCauseSystem").toString(),
                body.getOrDefault("mitigationSummary", "").toString(),
                startedAt
        ));
    }

    @PostMapping("/postmortems/{id}/guardrails")
    public ResponseEntity<PostmortemGuardrail> addGuardrail(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Instant dueAt = body.containsKey("dueAt") ? Instant.parse(body.get("dueAt").toString()) : null;
        return ResponseEntity.ok(postmortemService.addGuardrail(
                id,
                body.get("guardrailType").toString(),
                body.get("description").toString(),
                body.get("ownerTeam").toString(),
                dueAt
        ));
    }

    @PostMapping("/postmortems/guardrails/{guardrailId}/verify")
    public ResponseEntity<PostmortemGuardrail> verifyGuardrail(@PathVariable Long guardrailId) {
        return ResponseEntity.ok(postmortemService.verifyGuardrail(guardrailId));
    }

    @PostMapping("/postmortems/{id}/close")
    public ResponseEntity<IncidentPostmortem> closePostmortem(@PathVariable Long id) {
        return ResponseEntity.ok(postmortemService.closePostmortem(id));
    }
}
