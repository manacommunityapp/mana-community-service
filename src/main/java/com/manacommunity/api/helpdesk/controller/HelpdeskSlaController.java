package com.manacommunity.api.helpdesk.controller;

import com.manacommunity.api.helpdesk.entity.EscalationRule;
import com.manacommunity.api.helpdesk.entity.HelpdeskTicket;
import com.manacommunity.api.helpdesk.entity.Technician;
import com.manacommunity.api.helpdesk.entity.TowerDistance;
import com.manacommunity.api.helpdesk.repository.EscalationRuleRepository;
import com.manacommunity.api.helpdesk.repository.HelpdeskTicketRepository;
import com.manacommunity.api.helpdesk.repository.TechnicianRepository;
import com.manacommunity.api.helpdesk.repository.TowerDistanceRepository;
import com.manacommunity.api.helpdesk.service.DuplicateClusteringService;
import com.manacommunity.api.helpdesk.service.SlaEscalationService;
import com.manacommunity.api.helpdesk.service.TechnicianDispatchService;
import com.manacommunity.api.rbac.service.CrossSocietyTenantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/helpdesk-sla")
@RequiredArgsConstructor
public class HelpdeskSlaController {

    private final SlaEscalationService slaService;
    private final DuplicateClusteringService clusteringService;
    private final TechnicianDispatchService dispatchService;
    private final HelpdeskTicketRepository ticketRepository;
    private final EscalationRuleRepository ruleRepository;
    private final TechnicianRepository technicianRepository;
    private final TowerDistanceRepository distanceRepository;
    private final CrossSocietyTenantGuard tenantGuard;

    // === SLA Ticket Lifecycle ===

    @PostMapping("/tickets")
    public ResponseEntity<HelpdeskTicket> createTicketWithSla(@RequestBody HelpdeskTicket ticket) {
        tenantGuard.assertMatchesActiveTenant(ticket.getCommunityId());
        HelpdeskTicket created = slaService.createTicket(ticket);
        clusteringService.detectAndCluster(created);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<HelpdeskTicket>> getTickets(@RequestParam Long communityId) {
        tenantGuard.assertMatchesActiveTenant(communityId);
        return ResponseEntity.ok(ticketRepository.findByCommunityId(communityId));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<HelpdeskTicket> getTicket(@PathVariable Long id) {
        return ticketRepository.findById(id)
                .map(ticket -> {
                    tenantGuard.assertMatchesActiveTenant(ticket.getCommunityId());
                    return ResponseEntity.ok(ticket);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/tickets/{id}/resolve")
    public ResponseEntity<HelpdeskTicket> resolveTicket(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(slaService.resolveTicket(id, body.getOrDefault("resolutionNotes", "")));
    }

    // === SLA Escalation ===

    @PostMapping("/escalation/sweep")
    public ResponseEntity<SlaEscalationService.EscalationSweepResult> runEscalationSweep() {
        return ResponseEntity.ok(slaService.runEscalationSweep());
    }

    @PostMapping("/escalation/pause-check")
    public ResponseEntity<Map<String, String>> pauseOffHoursCheck() {
        slaService.pauseOffHoursTickets();
        return ResponseEntity.ok(Map.of("status", "Off-hours pause check completed"));
    }

    // === Duplicate Clustering ===

    @PostMapping("/clustering/detect")
    public ResponseEntity<DuplicateClusteringService.ClusterResult> detectDuplicates(
            @RequestBody HelpdeskTicket ticket) {
        return ResponseEntity.ok(clusteringService.detectAndCluster(ticket));
    }

    @GetMapping("/tickets/{id}/children")
    public ResponseEntity<List<HelpdeskTicket>> getChildTickets(@PathVariable Long id) {
        return ResponseEntity.ok(ticketRepository.findByParentIncidentId(id));
    }

    // === Technician Dispatch ===

    @PostMapping("/tickets/{id}/assign")
    public ResponseEntity<TechnicianDispatchService.DispatchResult> assignTechnician(@PathVariable Long id) {
        return ResponseEntity.ok(dispatchService.assignTechnician(id));
    }

    @PostMapping("/tickets/{ticketId}/emergency-override/{technicianId}")
    public ResponseEntity<TechnicianDispatchService.DispatchResult> emergencyOverride(
            @PathVariable Long ticketId, @PathVariable Long technicianId) {
        return ResponseEntity.ok(dispatchService.emergencyOverride(ticketId, technicianId));
    }

    @GetMapping("/technicians")
    public ResponseEntity<List<Technician>> getTechnicians(@RequestParam Long communityId) {
        return ResponseEntity.ok(dispatchService.getTechnicians(communityId));
    }

    // === Configuration ===

    @PostMapping("/escalation-rules")
    public ResponseEntity<EscalationRule> createEscalationRule(@RequestBody EscalationRule rule) {
        return ResponseEntity.ok(ruleRepository.save(rule));
    }

    @GetMapping("/escalation-rules")
    public ResponseEntity<List<EscalationRule>> getEscalationRules(
            @RequestParam Long communityId, @RequestParam String category) {
        return ResponseEntity.ok(ruleRepository
                .findByCommunityIdAndCategoryAndActiveTrueOrderByEscalationLevelAsc(communityId, category));
    }

    @PostMapping("/technicians")
    public ResponseEntity<Technician> createTechnician(@RequestBody Technician technician) {
        return ResponseEntity.ok(technicianRepository.save(technician));
    }

    @PostMapping("/tower-distances")
    public ResponseEntity<TowerDistance> createTowerDistance(@RequestBody TowerDistance distance) {
        return ResponseEntity.ok(distanceRepository.save(distance));
    }
}
