package com.manacommunity.api.helpdesk.service;

import com.manacommunity.api.helpdesk.entity.HelpdeskTicket;
import com.manacommunity.api.helpdesk.entity.Technician;
import com.manacommunity.api.helpdesk.entity.TowerDistance;
import com.manacommunity.api.helpdesk.repository.HelpdeskTicketRepository;
import com.manacommunity.api.helpdesk.repository.TechnicianRepository;
import com.manacommunity.api.helpdesk.repository.TowerDistanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicianDispatchService {

    private final TechnicianRepository technicianRepository;
    private final TowerDistanceRepository distanceRepository;
    private final HelpdeskTicketRepository ticketRepository;

    private static final Map<String, String> CATEGORY_TO_SKILL = Map.of(
            "PLUMBING", "PLUMBER",
            "ELECTRICAL", "ELECTRICIAN",
            "CARPENTRY", "CARPENTER",
            "WATER_LEAKAGE", "PLUMBER",
            "POWER_ISSUE", "ELECTRICIAN",
            "GENERAL", "GENERAL",
            "HOUSEKEEPING", "HOUSEKEEPING",
            "SECURITY", "SECURITY"
    );

    @Transactional
    public DispatchResult assignTechnician(Long ticketId) {
        HelpdeskTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        String requiredSkill = CATEGORY_TO_SKILL.getOrDefault(ticket.getCategory(), "GENERAL");
        LocalTime now = LocalTime.now();

        List<Technician> candidates = technicianRepository
                .findAvailableBySkill(ticket.getCommunityId(), requiredSkill);

        List<ScoredTechnician> scored = candidates.stream()
                .filter(t -> t.isOnShift(now))
                .filter(Technician::hasCapacity)
                .map(t -> scoreTechnician(t, ticket, requiredSkill))
                .sorted(Comparator.comparingDouble(ScoredTechnician::score))
                .collect(Collectors.toList());

        if (scored.isEmpty()) {
            List<Technician> allTechs = technicianRepository.findByCommunityIdAndActiveTrue(ticket.getCommunityId());
            scored = allTechs.stream()
                    .filter(t -> t.isOnShift(now))
                    .filter(t -> t.hasSkill(requiredSkill))
                    .map(t -> scoreTechnician(t, ticket, requiredSkill))
                    .sorted(Comparator.comparingDouble(ScoredTechnician::score))
                    .collect(Collectors.toList());
        }

        if (scored.isEmpty()) {
            return new DispatchResult(false, null, null, "No available technician with skill: " + requiredSkill, List.of());
        }

        ScoredTechnician best = scored.get(0);
        Technician tech = best.technician();

        ticket.setAssignedTechnicianId(tech.getId());
        ticket.setStatus("IN_PROGRESS");
        ticketRepository.save(ticket);

        tech.setActiveTicketCount(tech.getActiveTicketCount() + 1);
        technicianRepository.save(tech);

        log.info("Ticket {} assigned to {} (score {:.2f}, distance {}min, load {}/{})",
                ticketId, tech.getName(), best.score(), best.walkMinutes(),
                tech.getActiveTicketCount(), tech.getMaxConcurrentTickets());

        List<CandidateInfo> candidateInfos = scored.stream().limit(5)
                .map(s -> new CandidateInfo(s.technician().getId(), s.technician().getName(),
                        s.score(), s.walkMinutes(), s.workloadPct()))
                .collect(Collectors.toList());

        return new DispatchResult(true, tech.getId(), tech.getName(), "Assigned successfully", candidateInfos);
    }

    @Transactional
    public DispatchResult emergencyOverride(Long ticketId, Long technicianId) {
        HelpdeskTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        Technician tech = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new IllegalArgumentException("Technician not found"));

        if (ticket.getAssignedTechnicianId() != null) {
            Technician prev = technicianRepository.findById(ticket.getAssignedTechnicianId()).orElse(null);
            if (prev != null) {
                prev.setActiveTicketCount(Math.max(0, prev.getActiveTicketCount() - 1));
                technicianRepository.save(prev);
            }
        }

        ticket.setAssignedTechnicianId(tech.getId());
        ticket.setStatus("IN_PROGRESS");
        ticket.setPriority("CRITICAL");
        ticketRepository.save(ticket);

        tech.setActiveTicketCount(tech.getActiveTicketCount() + 1);
        technicianRepository.save(tech);

        log.warn("Emergency override: ticket {} force-assigned to {}", ticketId, tech.getName());
        return new DispatchResult(true, tech.getId(), tech.getName(), "Emergency override applied", List.of());
    }

    private ScoredTechnician scoreTechnician(Technician tech, HelpdeskTicket ticket, String requiredSkill) {
        int walkMinutes = getWalkMinutes(ticket.getCommunityId(), tech.getCurrentTower(), ticket.getTower());
        double workloadPct = (double) tech.getActiveTicketCount() / tech.getMaxConcurrentTickets();

        double distanceWeight = 0.4;
        double workloadWeight = 0.4;
        double skillWeight = 0.2;

        double distanceScore = walkMinutes / 15.0;
        double workloadScore = workloadPct;
        double skillScore = tech.getPrimarySkill().equalsIgnoreCase(requiredSkill) ? 0.0 : 0.5;

        double totalScore = (distanceScore * distanceWeight) + (workloadScore * workloadWeight) + (skillScore * skillWeight);
        return new ScoredTechnician(tech, totalScore, walkMinutes, workloadPct);
    }

    private int getWalkMinutes(Long communityId, String fromTower, String toTower) {
        if (fromTower == null || toTower == null || fromTower.equals(toTower)) return 0;
        return distanceRepository.findDistance(communityId, fromTower, toTower)
                .map(TowerDistance::getWalkMinutes)
                .orElse(10);
    }

    public List<Technician> getTechnicians(Long communityId) {
        return technicianRepository.findByCommunityIdAndActiveTrue(communityId);
    }

    private record ScoredTechnician(Technician technician, double score, int walkMinutes, double workloadPct) {}

    public record DispatchResult(boolean assigned, Long technicianId, String technicianName,
                                  String message, List<CandidateInfo> candidates) {}

    public record CandidateInfo(Long id, String name, double score, int walkMinutes, double workloadPct) {}
}
