package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CposCrmServiceImpl {

    private static final List<String> STAGE_ORDER =
            List.of("NEW", "CONTACTED", "QUALIFIED", "SITE_VISIT", "NEGOTIATION", "CLOSED");

    private final CposCrmLeadRepository leadRepo;
    private final CposCrmActivityRepository activityRepo;
    private final CposBrokerRepository brokerRepo;
    private final CposBrokerCommissionRepository commissionRepo;
    private final CommunityRepository communityRepo;

    // ── Leads ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CposCrmLeadDto> getLeads(AppUser user, String stage) {
        Long communityId = user.getCommunity().getId();
        List<CposCrmLead> leads = stage != null
                ? leadRepo.findByCommunityAndStage(communityId, stage, PageRequest.of(0, 200)).getContent()
                : leadRepo.findByCommunityIdAndDeletedFalseOrderByCreatedAtDesc(communityId);
        return leads.stream().map(this::toLeadDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CposCrmLeadDto getLeadById(AppUser user, Long id) {
        CposCrmLead lead = leadRepo.findByIdAndCommunity(id, user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
        CposCrmLeadDto dto = toLeadDto(lead);
        dto.setActivities(activityRepo.findByLeadIdOrderByCreatedAtDesc(id)
                .stream().map(this::toActivityDto).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public CposCrmLeadDto createLead(AppUser user, CposCrmLeadRequest req) {
        Community community = communityRepo.findById(user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        CposCrmLead lead = CposCrmLead.builder()
                .community(community)
                .leadName(req.getLeadName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .source(req.getSource() != null ? req.getSource() : "PORTAL")
                .leadType(req.getLeadType() != null ? req.getLeadType() : "APARTMENT")
                .budgetLakhs(req.getBudgetLakhs() != null ? req.getBudgetLakhs() : 0)
                .budgetDisplay(req.getBudgetDisplay())
                .assignedTo(req.getAssignedTo() != null ? req.getAssignedTo() : user.getFullName())
                .build();
        return toLeadDto(leadRepo.save(lead));
    }

    @Transactional
    public CposCrmLeadDto updateStage(AppUser user, Long id, String stage) {
        CposCrmLead lead = leadRepo.findByIdAndCommunity(id, user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
        int prevIdx = STAGE_ORDER.indexOf(lead.getStage());
        int newIdx  = STAGE_ORDER.indexOf(stage.toUpperCase());
        if (newIdx < 0) throw new IllegalArgumentException("Invalid stage: " + stage);
        // advance days_in_stage only on forward moves
        if (newIdx > prevIdx) {
            lead.setDaysTotal(lead.getDaysTotal() + lead.getDaysInStage());
            lead.setDaysInStage(0);
        }
        lead.setStage(stage.toUpperCase());
        lead.setStageChangedAt(LocalDateTime.now());

        // auto-approve broker commission when stage reaches CLOSED
        if ("CLOSED".equals(stage.toUpperCase())) {
            commissionRepo.findByCommunityId(user.getCommunity().getId()).stream()
                    .filter(bc -> bc.getLead().getId().equals(id) && "PENDING".equals(bc.getStatus()))
                    .forEach(bc -> {
                        bc.setStatus("APPROVED");
                        commissionRepo.save(bc);
                    });
        }

        return toLeadDto(leadRepo.save(lead));
    }

    @Transactional
    public CposCrmActivityDto addActivity(AppUser user, Long leadId, String type, String note) {
        CposCrmLead lead = leadRepo.findByIdAndCommunity(leadId, user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));
        CposCrmActivity activity = CposCrmActivity.builder()
                .lead(lead)
                .activityType(type.toUpperCase())
                .note(note)
                .createdBy(user.getFullName())
                .build();
        // bump days_in_stage
        lead.setDaysInStage(lead.getDaysInStage() + 1);
        leadRepo.save(lead);
        return toActivityDto(activityRepo.save(activity));
    }

    @Transactional
    public void deleteLead(AppUser user, Long id) {
        CposCrmLead lead = leadRepo.findByIdAndCommunity(id, user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
        lead.setDeleted(true);
        leadRepo.save(lead);
    }

    // ── Analytics ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CposCrmAnalyticsDto getAnalytics(AppUser user) {
        Long communityId = user.getCommunity().getId();
        List<CposCrmLead> allLeads = leadRepo.findByCommunityIdAndDeletedFalseOrderByCreatedAtDesc(communityId);

        long total = allLeads.size();
        long closed = allLeads.stream().filter(l -> "CLOSED".equals(l.getStage())).count();
        int conversionPct = total > 0 ? (int) Math.round((closed * 100.0) / total) : 0;
        double avgDays = total > 0 ? allLeads.stream().mapToInt(CposCrmLead::getDaysTotal).average().orElse(0) : 0;

        // AI score approximation: budget ≥ 200L = high score
        long hot = allLeads.stream().filter(l -> l.getBudgetLakhs() >= 200 && "NEGOTIATION".equals(l.getStage())).count()
                + allLeads.stream().filter(l -> "CLOSED".equals(l.getStage())).count();

        // Funnel: cumulative — leads that have reached each stage or beyond
        List<CposCrmAnalyticsDto.FunnelStageDto> funnel = STAGE_ORDER.stream().map(stage -> {
            int stageIdx = STAGE_ORDER.indexOf(stage);
            long count = allLeads.stream()
                    .filter(l -> STAGE_ORDER.indexOf(l.getStage()) >= stageIdx).count();
            int pct = total > 0 ? (int) Math.round((count * 100.0) / total) : 0;
            return CposCrmAnalyticsDto.FunnelStageDto.builder().stage(stage).count(count).pct(pct).build();
        }).collect(Collectors.toList());

        // Source breakdown
        Map<String, Long> sourceBreakdown = allLeads.stream()
                .collect(Collectors.groupingBy(CposCrmLead::getSource, Collectors.counting()));

        // Stage velocity
        Map<String, Double> stageVelocity = allLeads.stream()
                .collect(Collectors.groupingBy(CposCrmLead::getStage,
                        Collectors.averagingInt(CposCrmLead::getDaysInStage)));

        // Agent stats
        Map<String, List<CposCrmLead>> byAgent = allLeads.stream()
                .filter(l -> l.getAssignedTo() != null)
                .collect(Collectors.groupingBy(CposCrmLead::getAssignedTo));
        List<CposCrmAnalyticsDto.AgentStatDto> agentStats = byAgent.entrySet().stream().map(e -> {
            List<CposCrmLead> aLeads = e.getValue();
            long aClosed = aLeads.stream().filter(l -> "CLOSED".equals(l.getStage())).count();
            long aInNeg  = aLeads.stream().filter(l -> "NEGOTIATION".equals(l.getStage())).count();
            long pipeline = aLeads.stream().mapToLong(CposCrmLead::getBudgetLakhs).sum();
            int avgScore  = computeAvgScore(aLeads);
            return CposCrmAnalyticsDto.AgentStatDto.builder()
                    .agent(e.getKey()).totalLeads(aLeads.size())
                    .closed(aClosed).inNegotiation(aInNeg)
                    .avgScore(avgScore).pipelineLakhs(pipeline)
                    .build();
        }).collect(Collectors.toList());

        return CposCrmAnalyticsDto.builder()
                .totalLeads(total)
                .overallConversionPct(conversionPct)
                .avgDaysTotal(avgDays)
                .hotLeads(hot)
                .funnel(funnel)
                .sourceBreakdown(sourceBreakdown)
                .stageVelocity(stageVelocity)
                .agentStats(agentStats)
                .build();
    }

    // ── Brokers ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CposBrokerDto> getBrokers(AppUser user) {
        Long communityId = user.getCommunity().getId();
        return brokerRepo.findByCommunityIdAndActiveTrueOrderByBrokerNameAsc(communityId)
                .stream().map(this::toBrokerDto).collect(Collectors.toList());
    }

    @Transactional
    public CposBrokerDto createBroker(AppUser user, CposBrokerDto req) {
        Community community = communityRepo.findById(user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        CposBroker broker = CposBroker.builder()
                .community(community)
                .brokerName(req.getBrokerName())
                .firmName(req.getFirmName())
                .phone(req.getPhone())
                .email(req.getEmail())
                .commissionRate(req.getCommissionRate() != null ? req.getCommissionRate() : new BigDecimal("2.00"))
                .build();
        return toBrokerDto(brokerRepo.save(broker));
    }

    @Transactional
    public CposBrokerCommissionDto linkBrokerToLead(AppUser user, Long brokerId, Long leadId) {
        CposBroker broker = brokerRepo.findById(brokerId)
                .orElseThrow(() -> new ResourceNotFoundException("Broker not found: " + brokerId));
        CposCrmLead lead = leadRepo.findByIdAndCommunity(leadId, user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));
        BigDecimal commLakhs = broker.getCommissionRate()
                .multiply(BigDecimal.valueOf(lead.getBudgetLakhs()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        CposBrokerCommission bc = CposBrokerCommission.builder()
                .broker(broker).lead(lead)
                .commissionRate(broker.getCommissionRate())
                .commissionLakhs(commLakhs)
                .status("CLOSED".equals(lead.getStage()) ? "APPROVED" : "PENDING")
                .build();
        return toCommissionDto(commissionRepo.save(bc));
    }

    @Transactional(readOnly = true)
    public List<CposBrokerCommissionDto> getCommissions(AppUser user) {
        return commissionRepo.findByCommunityId(user.getCommunity().getId())
                .stream().map(this::toCommissionDto).collect(Collectors.toList());
    }

    @Transactional
    public CposBrokerCommissionDto approveCommission(AppUser user, Long commissionId) {
        CposBrokerCommission bc = commissionRepo.findById(commissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission not found: " + commissionId));
        bc.setStatus("APPROVED");
        return toCommissionDto(commissionRepo.save(bc));
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private CposCrmLeadDto toLeadDto(CposCrmLead l) {
        return CposCrmLeadDto.builder()
                .id(l.getId())
                .leadName(l.getLeadName())
                .email(l.getEmail())
                .phone(l.getPhone())
                .source(l.getSource())
                .stage(l.getStage())
                .leadType(l.getLeadType())
                .budgetLakhs(l.getBudgetLakhs())
                .budgetDisplay(l.getBudgetDisplay())
                .assignedTo(l.getAssignedTo())
                .daysInStage(l.getDaysInStage())
                .daysTotal(l.getDaysTotal())
                .aiScore(computeScore(l))
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    private CposCrmActivityDto toActivityDto(CposCrmActivity a) {
        return CposCrmActivityDto.builder()
                .id(a.getId())
                .activityType(a.getActivityType())
                .note(a.getNote())
                .createdBy(a.getCreatedBy())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private CposBrokerDto toBrokerDto(CposBroker b) {
        long closedLeads = b.getCommissions().stream()
                .filter(bc -> "CLOSED".equals(bc.getLead().getStage())).count();
        BigDecimal totalComm = b.getCommissions().stream()
                .map(CposBrokerCommission::getCommissionLakhs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CposBrokerDto.builder()
                .id(b.getId())
                .brokerName(b.getBrokerName())
                .firmName(b.getFirmName())
                .phone(b.getPhone())
                .email(b.getEmail())
                .commissionRate(b.getCommissionRate())
                .totalLeads(b.getCommissions().size())
                .closedLeads((int) closedLeads)
                .totalCommissionLakhs(totalComm)
                .build();
    }

    private CposBrokerCommissionDto toCommissionDto(CposBrokerCommission bc) {
        return CposBrokerCommissionDto.builder()
                .id(bc.getId())
                .brokerId(bc.getBroker().getId())
                .brokerName(bc.getBroker().getBrokerName())
                .firmName(bc.getBroker().getFirmName())
                .leadId(bc.getLead().getId())
                .leadName(bc.getLead().getLeadName())
                .budgetLakhs(bc.getLead().getBudgetLakhs())
                .leadStage(bc.getLead().getStage())
                .commissionRate(bc.getCommissionRate())
                .commissionLakhs(bc.getCommissionLakhs())
                .status(bc.getStatus())
                .createdAt(bc.getCreatedAt())
                .build();
    }

    private int computeScore(CposCrmLead l) {
        int budget = l.getBudgetLakhs() >= 200 ? 25 : l.getBudgetLakhs() >= 100 ? 20 : l.getBudgetLakhs() >= 60 ? 14 : 8;
        int stageIdx = STAGE_ORDER.indexOf(l.getStage());
        int stagePts = stageIdx >= 0 ? Math.round((float) stageIdx / (STAGE_ORDER.size() - 1) * 25) : 0;
        int velocity = l.getDaysInStage() <= 2 ? 10 : l.getDaysInStage() <= 5 ? 7 : l.getDaysInStage() <= 10 ? 4 : 1;
        int source = ("WALK_IN".equals(l.getSource()) || "REFERRAL".equals(l.getSource())) ? 15
                   : "BROKER".equals(l.getSource()) ? 12 : 8;
        return Math.min(100, budget + stagePts + velocity + source);
    }

    private int computeAvgScore(List<CposCrmLead> leads) {
        return leads.isEmpty() ? 0 :
                (int) Math.round(leads.stream().mapToInt(this::computeScore).average().orElse(0));
    }
}
