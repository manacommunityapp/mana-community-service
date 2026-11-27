package com.manacommunity.api.architecture.controller;

import com.manacommunity.api.architecture.service.BuildVsBuyAnalysisService;
import com.manacommunity.api.architecture.service.PersonaDesignSystemService;
import com.manacommunity.api.architecture.service.StranglerExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/architecture")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ArchitectureShowcaseController {

    private final StranglerExtractionService stranglerService;
    private final BuildVsBuyAnalysisService buildVsBuyService;
    private final PersonaDesignSystemService personaService;

    // === Monolith-to-Microservices ===

    @PostMapping("/strangler/plan")
    public ResponseEntity<StranglerExtractionService.ExtractionPlan> generateExtractionPlan(
            @RequestBody StranglerRequest req) {
        return ResponseEntity.ok(stranglerService.generateExtractionPlan(
                req.sourceDomain(), req.targetDomain(), req.sharedTables(),
                req.dailyTransactions(), req.teamSize(), req.featureFreezeAllowed()));
    }

    @PostMapping("/strangler/simulate")
    public ResponseEntity<StranglerExtractionService.LatencySimulation> simulateLatency(
            @RequestBody LatencySimRequest req) {
        return ResponseEntity.ok(stranglerService.simulateBillingImpact(
                req.gateRequestsPerSec(), req.billingCronQueriesPerSec(),
                req.sharedDbConnections(), req.extracted()));
    }

    // === Build vs Buy ===

    @PostMapping("/build-vs-buy/analyze")
    public ResponseEntity<BuildVsBuyAnalysisService.BuildVsBuyDecision> analyzeBuildVsBuy(
            @RequestBody BuildVsBuyRequest req) {
        return ResponseEntity.ok(buildVsBuyService.analyze(
                req.dailyCalls(), req.avgCallDurationSec(), req.engineeringTeamSize(),
                req.traiDltRequired(), req.targetLatencyMs()));
    }

    // === Persona Design System ===

    @GetMapping("/personas/plan")
    public ResponseEntity<PersonaDesignSystemService.DesignSystemPlan> getDesignSystemPlan() {
        return ResponseEntity.ok(personaService.generatePlan());
    }

    @PostMapping("/personas/api-analysis")
    public ResponseEntity<PersonaDesignSystemService.PersonaApiAnalysis> analyzeApi(
            @RequestBody ApiAnalysisRequest req) {
        return ResponseEntity.ok(personaService.analyzeApiComplexity(
                req.persona(), req.totalEndpoints(), req.sharedEndpoints(), req.personaSpecific()));
    }

    // Request DTOs
    public record StranglerRequest(String sourceDomain, String targetDomain, List<String> sharedTables,
                                    int dailyTransactions, int teamSize, boolean featureFreezeAllowed) {}
    public record LatencySimRequest(int gateRequestsPerSec, int billingCronQueriesPerSec,
                                     int sharedDbConnections, boolean extracted) {}
    public record BuildVsBuyRequest(long dailyCalls, double avgCallDurationSec, int engineeringTeamSize,
                                     boolean traiDltRequired, int targetLatencyMs) {}
    public record ApiAnalysisRequest(String persona, int totalEndpoints, int sharedEndpoints, int personaSpecific) {}
}
