package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.notification.provider.SmsProvider;
import com.manacommunity.api.notification.provider.SmsSendRequest;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.storage.FileStorageService;
import com.manacommunity.api.storage.StoredFileDto;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final CposCrmInboundConfigRepository inboundConfigRepo;
    private final CposCrmDocumentRepository documentRepo;
    private final FileStorageService fileStorageService;
    private final CposCrmEmailTemplateRepository emailTemplateRepo;
    private final EmailService emailService;
    private final AppUserRepository appUserRepo;
    private final ExpoPushService expoPushService;
    private final CposCrmTagRepository tagRepo;
    private final CposCrmSalesTargetRepository salesTargetRepo;
    private final CposCrmEmailCampaignRepository emailCampaignRepo;
    private final SmsProvider smsProvider;
    private final CposCrmDripSequenceRepository dripSequenceRepo;
    private final CposCrmDripStepRepository dripStepRepo;

    // ── RBAC helpers ──────────────────────────────────────────────────────

    private boolean isCrmAdmin(AppUser user) {
        return user.hasRole("ADMIN") || user.hasRole("MANAGER");
    }

    public String getCrmRole(AppUser user) {
        return isCrmAdmin(user) ? "ADMIN" : "AGENT";
    }

    // ── Leads ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CposCrmLeadDto> getLeads(AppUser user, String stage) {
        Long communityId = user.getCommunity().getId();
        List<CposCrmLead> leads;
        if (isCrmAdmin(user)) {
            leads = stage != null
                    ? leadRepo.findByCommunityAndStage(communityId, stage, PageRequest.of(0, 200)).getContent()
                    : leadRepo.findByCommunityIdAndDeletedFalseOrderByCreatedAtDesc(communityId);
        } else {
            String agentName = user.getFullName() != null ? user.getFullName() : user.getEmail();
            leads = stage != null
                    ? leadRepo.findByCommunityAndAssignedToAndStage(communityId, agentName, stage, PageRequest.of(0, 200)).getContent()
                    : leadRepo.findByCommunityIdAndAssignedToAndDeletedFalseOrderByCreatedAtDesc(communityId, agentName);
        }
        return leads.stream().map(this::toLeadDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getAgents(AppUser user) {
        return leadRepo.findDistinctAgents(user.getCommunity().getId());
    }

    @Transactional
    public CposCrmLeadDto assignLead(AppUser admin, Long leadId, String agentName) {
        if (!isCrmAdmin(admin)) {
            throw new com.manacommunity.api.exception.ResourceNotFoundException("Access denied");
        }
        CposCrmLead lead = leadRepo.findByIdAndCommunity(leadId, admin.getCommunity().getId())
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("Lead not found: " + leadId));
        lead.setAssignedTo(agentName);
        CposCrmLeadDto result = toLeadDto(leadRepo.save(lead));
        String adminName = admin.getFullName() != null ? admin.getFullName() : admin.getEmail();
        expoPushService.notifyCrmLeadAssigned(
                findAgentUserId(admin.getCommunity().getId(), agentName).orElse(null),
                lead.getLeadName(), adminName, lead.getId());
        return result;
    }

    private java.util.Optional<Long> findAgentUserId(Long communityId, String agentName) {
        if (agentName == null || agentName.isBlank()) return java.util.Optional.empty();
        return appUserRepo.findFirstByCommunityIdAndFullNameIgnoreCase(communityId, agentName)
                .map(AppUser::getId);
    }

    // ── Document Vault ────────────────────────────────────────────────────

    @Transactional
    public CposCrmDocumentDto uploadDocument(AppUser user, Long leadId, MultipartFile file) {
        Long communityId = user.getCommunity().getId();
        CposCrmLead lead = leadRepo.findByIdAndCommunity(leadId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));
        String customPath = String.format("crm/%d/%d", communityId, leadId);
        StoredFileDto stored = fileStorageService.store(file, user.getId(), customPath);
        String uploader = user.getFullName() != null ? user.getFullName() : user.getEmail();
        CposCrmDocument doc = CposCrmDocument.builder()
                .lead(lead)
                .communityId(communityId)
                .fileName(stored.originalName())
                .fileUrl(stored.url())
                .fileSize(stored.sizeBytes())
                .mimeType(stored.contentType())
                .uploadedBy(uploader)
                .build();
        return toDocDto(documentRepo.save(doc));
    }

    @Transactional(readOnly = true)
    public List<CposCrmDocumentDto> getDocuments(AppUser user, Long leadId) {
        return documentRepo
                .findByLeadIdAndCommunityIdOrderByCreatedAtDesc(leadId, user.getCommunity().getId())
                .stream().map(this::toDocDto).collect(Collectors.toList());
    }

    @Transactional
    public void deleteDocument(AppUser user, Long leadId, Long docId) {
        CposCrmDocument doc = documentRepo.findByIdAndCommunityId(docId, user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docId));
        if (!doc.getLead().getId().equals(leadId)) {
            throw new ResourceNotFoundException("Document not found: " + docId);
        }
        documentRepo.delete(doc);
    }

    // ── CSV Export ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String exportLeadsCsv(AppUser user, String stage) {
        Long communityId = user.getCommunity().getId();
        List<CposCrmLead> leads;
        if (isCrmAdmin(user)) {
            leads = stage != null
                    ? leadRepo.findByCommunityAndStage(communityId, stage, PageRequest.of(0, 5000)).getContent()
                    : leadRepo.findByCommunityIdAndDeletedFalseOrderByCreatedAtDesc(communityId);
        } else {
            String agentName = user.getFullName() != null ? user.getFullName() : user.getEmail();
            leads = stage != null
                    ? leadRepo.findByCommunityAndAssignedToAndStage(communityId, agentName, stage, PageRequest.of(0, 5000)).getContent()
                    : leadRepo.findByCommunityIdAndAssignedToAndDeletedFalseOrderByCreatedAtDesc(communityId, agentName);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Lead Name,Email,Phone,Source,Stage,Lead Type,Budget (L),Assigned To,AI Score,Days in Pipeline,Follow-up At,Created At\n");
        for (CposCrmLead l : leads) {
            sb.append(csvField(String.valueOf(l.getId()))).append(',');
            sb.append(csvField(l.getLeadName())).append(',');
            sb.append(csvField(l.getEmail())).append(',');
            sb.append(csvField(l.getPhone())).append(',');
            sb.append(csvField(l.getSource())).append(',');
            sb.append(csvField(l.getStage())).append(',');
            sb.append(csvField(l.getLeadType())).append(',');
            sb.append(l.getBudgetLakhs()).append(',');
            sb.append(csvField(l.getAssignedTo())).append(',');
            sb.append(computeScore(l)).append(',');
            sb.append(l.getDaysTotal()).append(',');
            sb.append(csvField(l.getFollowUpAt() != null ? l.getFollowUpAt().toString() : "")).append(',');
            sb.append(csvField(l.getCreatedAt() != null ? l.getCreatedAt().toString() : "")).append('\n');
        }
        return sb.toString();
    }

    private static String csvField(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private CposCrmDocumentDto toDocDto(CposCrmDocument d) {
        return CposCrmDocumentDto.builder()
                .id(d.getId())
                .leadId(d.getLead().getId())
                .fileName(d.getFileName())
                .fileUrl(d.getFileUrl())
                .fileSize(d.getFileSize())
                .mimeType(d.getMimeType())
                .uploadedBy(d.getUploadedBy())
                .createdAt(d.getCreatedAt())
                .build();
    }

    // ── Email Templates ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CposCrmEmailTemplateDto> getEmailTemplates(AppUser user) {
        return emailTemplateRepo.findByCommunityIdOrderByCreatedAtDesc(user.getCommunity().getId())
                .stream().map(this::toEmailTemplateDto).collect(Collectors.toList());
    }

    @Transactional
    public CposCrmEmailTemplateDto createEmailTemplate(AppUser user, Map<String, String> req) {
        String author = user.getFullName() != null ? user.getFullName() : user.getEmail();
        CposCrmEmailTemplate tpl = CposCrmEmailTemplate.builder()
                .communityId(user.getCommunity().getId())
                .name(req.getOrDefault("name", "Untitled"))
                .subject(req.getOrDefault("subject", ""))
                .body(req.getOrDefault("body", ""))
                .createdBy(author)
                .build();
        return toEmailTemplateDto(emailTemplateRepo.save(tpl));
    }

    @Transactional
    public CposCrmEmailTemplateDto updateEmailTemplate(AppUser user, Long templateId, Map<String, String> req) {
        CposCrmEmailTemplate tpl = emailTemplateRepo.findByIdAndCommunityId(templateId, user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        if (req.containsKey("name")) tpl.setName(req.get("name"));
        if (req.containsKey("subject")) tpl.setSubject(req.get("subject"));
        if (req.containsKey("body")) tpl.setBody(req.get("body"));
        return toEmailTemplateDto(emailTemplateRepo.save(tpl));
    }

    @Transactional
    public void deleteEmailTemplate(AppUser user, Long templateId) {
        CposCrmEmailTemplate tpl = emailTemplateRepo.findByIdAndCommunityId(templateId, user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        emailTemplateRepo.delete(tpl);
    }

    @Transactional
    public CposCrmActivityDto sendLeadEmail(AppUser user, Long leadId, Map<String, String> req) {
        Long communityId = user.getCommunity().getId();
        CposCrmLead lead = leadRepo.findByIdAndCommunity(leadId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));

        String to = req.get("to");
        if (to == null || to.isBlank()) {
            to = lead.getEmail();
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Lead has no email address");
        }
        String agentName = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String subject = interpolate(req.getOrDefault("subject", "(no subject)"), lead, agentName);
        String htmlBody = interpolate(req.getOrDefault("body", ""), lead, agentName);

        emailService.send(EmailMessage.builder()
                .to(to)
                .toName(lead.getLeadName())
                .subject(subject)
                .htmlBody(wrapHtml(htmlBody, subject))
                .build());

        CposCrmActivity activity = CposCrmActivity.builder()
                .lead(lead)
                .activityType("EMAIL")
                .note("Email sent: " + subject)
                .createdBy(agentName)
                .build();
        return toActivityDto(activityRepo.save(activity));
    }

    private String interpolate(String text, CposCrmLead lead, String agentName) {
        return text
                .replace("{{leadName}}", lead.getLeadName() != null ? lead.getLeadName() : "")
                .replace("{{agentName}}", agentName != null ? agentName : "")
                .replace("{{phone}}", lead.getPhone() != null ? lead.getPhone() : "")
                .replace("{{email}}", lead.getEmail() != null ? lead.getEmail() : "");
    }

    private String wrapHtml(String body, String subject) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + subject +
               "</title></head><body style=\"font-family:Arial,sans-serif;color:#222;\">" +
               body + "</body></html>";
    }

    private CposCrmEmailTemplateDto toEmailTemplateDto(CposCrmEmailTemplate t) {
        return CposCrmEmailTemplateDto.builder()
                .id(t.getId())
                .name(t.getName())
                .subject(t.getSubject())
                .body(t.getBody())
                .createdBy(t.getCreatedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
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

        if (newIdx > prevIdx && lead.getAssignedTo() != null) {
            expoPushService.notifyCrmStageChanged(
                    findAgentUserId(user.getCommunity().getId(), lead.getAssignedTo()).orElse(null),
                    lead.getLeadName(), stage.toUpperCase(), lead.getId());
        }

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

        // Call outcome breakdown
        List<CposCrmAnalyticsDto.CallOutcomeDto> callOutcomes = activityRepo.countCallOutcomesByCommunity(communityId)
                .stream().map(row -> CposCrmAnalyticsDto.CallOutcomeDto.builder()
                        .outcome((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        // Hourly activity heatmap (0–23)
        Map<Integer, Long> hourMap = activityRepo.countActivitiesByHour(communityId)
                .stream().collect(Collectors.toMap(r -> ((Number) r[0]).intValue(), r -> ((Number) r[1]).longValue()));
        List<CposCrmAnalyticsDto.HourlyActivityDto> hourlyActivity = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hourlyActivity.add(CposCrmAnalyticsDto.HourlyActivityDto.builder()
                    .hour(h).count(hourMap.getOrDefault(h, 0L)).build());
        }

        // Sales forecast: probability-weighted pipeline
        Map<String, Integer> stageProbability = Map.of(
                "NEW", 5, "CONTACTED", 15, "QUALIFIED", 35, "SITE_VISIT", 55, "NEGOTIATION", 75, "CLOSED", 100);
        List<CposCrmAnalyticsDto.StageForecastDto> stageForecast = STAGE_ORDER.stream().map(stage -> {
            List<CposCrmLead> stageLeads = allLeads.stream()
                    .filter(l -> stage.equals(l.getStage())).collect(Collectors.toList());
            int prob = stageProbability.getOrDefault(stage, 0);
            long weighted = stageLeads.stream()
                    .mapToLong(l -> Math.round(l.getBudgetLakhs() * prob / 100.0)).sum();
            return CposCrmAnalyticsDto.StageForecastDto.builder()
                    .stage(stage).leads(stageLeads.size())
                    .probabilityPct(prob).weightedRevenueLakhs(weighted).build();
        }).collect(Collectors.toList());
        long totalWeighted = stageForecast.stream().mapToLong(CposCrmAnalyticsDto.StageForecastDto::getWeightedRevenueLakhs).sum();
        long pipelineLeads = allLeads.stream().filter(l -> !"CLOSED".equals(l.getStage())).count();

        return CposCrmAnalyticsDto.builder()
                .totalLeads(total)
                .overallConversionPct(conversionPct)
                .avgDaysTotal(avgDays)
                .hotLeads(hot)
                .funnel(funnel)
                .sourceBreakdown(sourceBreakdown)
                .stageVelocity(stageVelocity)
                .agentStats(agentStats)
                .callOutcomes(callOutcomes)
                .hourlyActivity(hourlyActivity)
                .forecast(CposCrmAnalyticsDto.ForecastDto.builder()
                        .weightedRevenueLakhs(totalWeighted)
                        .totalPipelineLeads(pipelineLeads)
                        .byStage(stageForecast).build())
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
        List<CposCrmLeadDto.TagDto> tagDtos = l.getTags() == null ? List.of() :
                l.getTags().stream()
                        .map(t -> CposCrmLeadDto.TagDto.builder()
                                .id(t.getId()).name(t.getName()).color(t.getColor()).build())
                        .collect(Collectors.toList());
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
                .followUpAt(l.getFollowUpAt())
                .followUpNote(l.getFollowUpNote())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .tags(tagDtos)
                .approvalStatus(l.getApprovalStatus())
                .approvalReason(l.getApprovalReason())
                .approvalComment(l.getApprovalComment())
                .build();
    }

    private CposCrmActivityDto toActivityDto(CposCrmActivity a) {
        return CposCrmActivityDto.builder()
                .id(a.getId())
                .activityType(a.getActivityType())
                .note(a.getNote())
                .createdBy(a.getCreatedBy())
                .callOutcome(a.getCallOutcome())
                .callDurationMin(a.getCallDurationMin())
                .createdAt(a.getCreatedAt())
                .build();
    }

    @Transactional
    public CposCrmActivityDto logCall(AppUser user, Long leadId,
                                      String outcome, Integer durationMin,
                                      String notes, LocalDateTime followUpAt) {
        CposCrmLead lead = leadRepo.findByIdAndCommunity(leadId, user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));

        String noteText = buildCallNote(outcome, durationMin, notes);
        CposCrmActivity activity = CposCrmActivity.builder()
                .lead(lead)
                .activityType("CALL")
                .note(noteText)
                .createdBy(user.getFullName() != null ? user.getFullName() : user.getEmail())
                .callOutcome(outcome)
                .callDurationMin(durationMin)
                .build();
        activityRepo.save(activity);

        if (followUpAt != null) {
            String followUpNote = "MISSED".equals(outcome) ? "Missed call — scheduled callback"
                    : "Busy — scheduled callback";
            lead.setFollowUpAt(followUpAt);
            lead.setFollowUpNote(followUpNote);
            leadRepo.save(lead);
        }

        return toActivityDto(activity);
    }

    private String buildCallNote(String outcome, Integer durationMin, String notes) {
        StringBuilder sb = new StringBuilder();
        if ("CONNECTED".equals(outcome)) sb.append("Call connected");
        else if ("MISSED".equals(outcome)) sb.append("Call missed");
        else if ("BUSY".equals(outcome)) sb.append("Line busy");
        else sb.append("Call");
        if (durationMin != null && durationMin > 0) sb.append(" (").append(durationMin).append(" min)");
        if (notes != null && !notes.isBlank()) sb.append(": ").append(notes.trim());
        return sb.toString();
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

    @Transactional
    public CposCrmLeadDto setFollowUp(AppUser user, Long id, LocalDateTime followUpAt, String note) {
        CposCrmLead lead = leadRepo.findByIdAndCommunity(id, user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
        lead.setFollowUpAt(followUpAt);
        lead.setFollowUpNote(note);
        return toLeadDto(leadRepo.save(lead));
    }

    @Transactional(readOnly = true)
    public List<CposCrmLeadDto> getDueFollowUps(AppUser user) {
        LocalDateTime now = LocalDateTime.now();
        return leadRepo.findByCommunityIdAndDeletedFalseOrderByCreatedAtDesc(user.getCommunity().getId())
                .stream()
                .filter(l -> l.getFollowUpAt() != null && !l.getFollowUpAt().isAfter(now))
                .map(this::toLeadDto)
                .collect(Collectors.toList());
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

    // ── Portal Inbound Integration ─────────────────────────────────────────

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Transactional
    public CposCrmInboundConfigDto getOrCreateInboundConfig(AppUser user) {
        Long communityId = user.getCommunity().getId();
        CposCrmInboundConfig cfg = inboundConfigRepo.findByCommunityId(communityId)
                .orElseGet(() -> {
                    Community community = communityRepo.findById(communityId)
                            .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
                    String key = java.util.UUID.randomUUID().toString().replace("-", "");
                    CposCrmInboundConfig newCfg = CposCrmInboundConfig.builder()
                            .community(community)
                            .apiKey(key)
                            .agentPool("[]")
                            .build();
                    return inboundConfigRepo.save(newCfg);
                });
        long inboundCount = leadRepo.findByCommunityIdAndDeletedFalseOrderByCreatedAtDesc(communityId)
                .stream().filter(l -> "PORTAL".equals(l.getSource())).count();
        return toCfgDto(cfg, inboundCount);
    }

    @Transactional
    public CposCrmInboundConfigDto updateInboundConfig(AppUser user, List<String> agentPool) {
        CposCrmInboundConfig cfg = inboundConfigRepo.findByCommunityId(user.getCommunity().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Integration config not found"));
        try {
            cfg.setAgentPool(MAPPER.writeValueAsString(agentPool));
            cfg.setUpdatedAt(LocalDateTime.now());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid agent pool", e);
        }
        inboundConfigRepo.save(cfg);
        return toCfgDto(cfg, 0L);
    }

    @Transactional
    public CposCrmLeadDto createInboundLead(String apiKey, CposCrmInboundLeadRequest req) {
        CposCrmInboundConfig cfg = inboundConfigRepo.findByApiKey(apiKey)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid API key"));
        Community community = cfg.getCommunity();

        List<String> pool = parsePool(cfg.getAgentPool());
        String assignedTo = pool.isEmpty() ? null : pool.get(cfg.getRrIndex() % pool.size());
        if (!pool.isEmpty()) {
            cfg.setRrIndex((cfg.getRrIndex() + 1) % pool.size());
            inboundConfigRepo.save(cfg);
        }

        double budgetLakhsRaw = req.getBudgetLakhs() != null ? req.getBudgetLakhs() : 0;
        int budgetLakhs = (int) budgetLakhsRaw;
        CposCrmLead lead = CposCrmLead.builder()
                .community(community)
                .leadName(req.getName() != null ? req.getName() : "Unknown")
                .phone(req.getPhone())
                .email(req.getEmail())
                .source("PORTAL")
                .stage("NEW")
                .leadType(req.getPropertyType() != null ? req.getPropertyType() : "Residential")
                .budgetLakhs(budgetLakhs)
                .budgetDisplay(budgetLakhsRaw > 0 ? String.format("₹%.0f L", budgetLakhsRaw) : null)
                .assignedTo(assignedTo)
                .daysInStage(0)
                .daysTotal(0)
                .build();
        CposCrmLead saved = leadRepo.save(lead);

        String portal = req.getPortal() != null ? req.getPortal() : "PORTAL";
        String note = String.format("Inbound lead from %s%s", portal,
                req.getPortalLeadId() != null ? " (Ref: " + req.getPortalLeadId() + ")" : "");
        CposCrmActivity activity = CposCrmActivity.builder()
                .lead(saved)
                .activityType("NOTE")
                .note(note)
                .createdBy("System")
                .build();
        activityRepo.save(activity);

        if (assignedTo != null) {
            expoPushService.notifyCrmNewInboundLead(
                    findAgentUserId(community.getId(), assignedTo).orElse(null),
                    saved.getLeadName(), portal, saved.getId());
        }

        return toLeadDto(saved);
    }

    private List<String> parsePool(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private CposCrmInboundConfigDto toCfgDto(CposCrmInboundConfig cfg, long inboundCount) {
        return CposCrmInboundConfigDto.builder()
                .id(cfg.getId())
                .apiKey(cfg.getApiKey())
                .agentPool(parsePool(cfg.getAgentPool()))
                .rrIndex(cfg.getRrIndex())
                .totalInboundLeads(inboundCount)
                .build();
    }

    // ── Tags ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CposCrmTagDto> getTags(AppUser user) {
        return tagRepo.findByCommunityId(user.getCommunity().getId())
                .stream().map(t -> CposCrmTagDto.builder()
                        .id(t.getId()).name(t.getName()).color(t.getColor()).build())
                .collect(Collectors.toList());
    }

    @Transactional
    public CposCrmTagDto createTag(AppUser user, String name, String color) {
        Long communityId = user.getCommunity().getId();
        if (tagRepo.existsByCommunityIdAndNameIgnoreCase(communityId, name)) {
            throw new IllegalArgumentException("Tag already exists: " + name);
        }
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        CposCrmTag tag = CposCrmTag.builder()
                .community(community)
                .name(name)
                .color(color != null ? color : "#4F6AF5")
                .build();
        CposCrmTag saved = tagRepo.save(tag);
        return CposCrmTagDto.builder().id(saved.getId()).name(saved.getName()).color(saved.getColor()).build();
    }

    @Transactional
    public void deleteTag(AppUser user, Long tagId) {
        CposCrmTag tag = tagRepo.findByCommunityIdAndId(user.getCommunity().getId(), tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));
        tagRepo.delete(tag);
    }

    @Transactional
    public CposCrmLeadDto addTagToLead(AppUser user, Long leadId, Long tagId) {
        Long communityId = user.getCommunity().getId();
        CposCrmLead lead = leadRepo.findByIdAndCommunity(leadId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));
        CposCrmTag tag = tagRepo.findByCommunityIdAndId(communityId, tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));
        lead.getTags().add(tag);
        return toLeadDto(leadRepo.save(lead));
    }

    @Transactional
    public CposCrmLeadDto removeTagFromLead(AppUser user, Long leadId, Long tagId) {
        Long communityId = user.getCommunity().getId();
        CposCrmLead lead = leadRepo.findByIdAndCommunity(leadId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));
        lead.getTags().removeIf(t -> t.getId().equals(tagId));
        return toLeadDto(leadRepo.save(lead));
    }

    // ── Lead Deduplication + CSV Import ───────────────────────────────────

    @Transactional(readOnly = true)
    public boolean isDuplicateLead(Long communityId, String phone, String email) {
        if (phone != null && !phone.isBlank() &&
                leadRepo.existsByCommunityIdAndPhoneAndDeletedFalse(communityId, phone)) return true;
        if (email != null && !email.isBlank() &&
                leadRepo.existsByCommunityIdAndEmailAndDeletedFalse(communityId, email)) return true;
        return false;
    }

    @Transactional
    public CrmImportResultDto importLeadsCsv(AppUser user, String csvContent) {
        Long communityId = user.getCommunity().getId();
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        int created = 0, duplicates = 0, errors = 0;
        List<String> errorMessages = new ArrayList<>();

        String[] lines = csvContent.split("\n");
        if (lines.length < 2) {
            return CrmImportResultDto.builder().created(0).duplicates(0).errors(1)
                    .errorMessages(List.of("CSV has no data rows")).build();
        }

        // parse header row
        String[] headers = lines[0].trim().split(",");
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            idx.put(headers[i].trim().toLowerCase().replace(" ", "_").replace("(", "").replace(")", ""), i);
        }

        for (int row = 1; row < lines.length; row++) {
            String line = lines[row].trim();
            if (line.isEmpty()) continue;
            try {
                String[] cols = parseCsvRow(line);
                String leadName = getCol(cols, idx, "lead_name", "name");
                String phone    = getCol(cols, idx, "phone");
                String email    = getCol(cols, idx, "email");

                if (leadName == null || leadName.isBlank()) {
                    errorMessages.add("Row " + row + ": missing lead name");
                    errors++;
                    continue;
                }
                if (isDuplicateLead(communityId, phone, email)) {
                    duplicates++;
                    continue;
                }

                String source = getCol(cols, idx, "source");
                String stage  = getCol(cols, idx, "stage");
                String budget = getCol(cols, idx, "budget_l", "budget");
                int budgetLakhs = 0;
                if (budget != null && !budget.isBlank()) {
                    try { budgetLakhs = (int) Double.parseDouble(budget); } catch (NumberFormatException ignored) {}
                }

                CposCrmLead lead = CposCrmLead.builder()
                        .community(community)
                        .leadName(leadName)
                        .phone(phone)
                        .email(email)
                        .source(source != null ? source.toUpperCase() : "PORTAL")
                        .stage(stage != null && STAGE_ORDER.contains(stage.toUpperCase()) ? stage.toUpperCase() : "NEW")
                        .leadType("APARTMENT")
                        .budgetLakhs(budgetLakhs)
                        .budgetDisplay(budgetLakhs > 0 ? String.format("₹%d L", budgetLakhs) : null)
                        .assignedTo(getCol(cols, idx, "assigned_to"))
                        .build();
                leadRepo.save(lead);
                created++;
            } catch (Exception e) {
                errorMessages.add("Row " + row + ": " + e.getMessage());
                errors++;
            }
        }
        return CrmImportResultDto.builder()
                .created(created).duplicates(duplicates).errors(errors)
                .errorMessages(errorMessages).build();
    }

    private String getCol(String[] cols, Map<String, Integer> idx, String... keys) {
        for (String key : keys) {
            Integer i = idx.get(key);
            if (i != null && i < cols.length) {
                String v = cols[i].trim().replaceAll("^\"|\"$", "");
                if (!v.isBlank()) return v;
            }
        }
        return null;
    }

    private String[] parseCsvRow(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ',' && !inQuotes) { cols.add(cur.toString()); cur.setLength(0); }
            else { cur.append(c); }
        }
        cols.add(cur.toString());
        return cols.toArray(new String[0]);
    }

    // ── Deal Rotting Alerts ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CposCrmLeadDto> getStaleLeads(AppUser user, int staleDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(staleDays);
        return leadRepo.findStaleLeads(user.getCommunity().getId(), cutoff)
                .stream().map(this::toLeadDto).collect(Collectors.toList());
    }

    // ── Sales Targets + Leaderboard ───────────────────────────────────────

    @Transactional
    public CposCrmSalesTargetDto upsertSalesTarget(AppUser user, String agentName,
                                                    int month, int year,
                                                    int targetLeadsClosed, int targetRevenueLakhs) {
        if (!isCrmAdmin(user)) throw new ResourceNotFoundException("Admin only");
        Long communityId = user.getCommunity().getId();
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        CposCrmSalesTarget target = salesTargetRepo
                .findByCommunityIdAndAgentNameAndPeriodMonthAndPeriodYear(communityId, agentName, month, year)
                .orElseGet(() -> CposCrmSalesTarget.builder()
                        .community(community).agentName(agentName)
                        .periodMonth(month).periodYear(year).build());
        target.setTargetLeadsClosed(targetLeadsClosed);
        target.setTargetRevenueLakhs(targetRevenueLakhs);
        CposCrmSalesTarget saved = salesTargetRepo.save(target);
        return toTargetDto(communityId, saved);
    }

    @Transactional(readOnly = true)
    public List<CposCrmSalesTargetDto> getSalesTargets(AppUser user, int month, int year) {
        Long communityId = user.getCommunity().getId();
        return salesTargetRepo.findByCommunityIdAndPeriodMonthAndPeriodYear(communityId, month, year)
                .stream().map(t -> toTargetDto(communityId, t)).collect(Collectors.toList());
    }

    private CposCrmSalesTargetDto toTargetDto(Long communityId, CposCrmSalesTarget t) {
        long actualClosed = leadRepo.countClosedByAgentInMonth(communityId, t.getAgentName(),
                t.getPeriodMonth(), t.getPeriodYear());
        Long actualRevRaw = leadRepo.sumRevenueByAgentInMonth(communityId, t.getAgentName(),
                t.getPeriodMonth(), t.getPeriodYear());
        long actualRev = actualRevRaw != null ? actualRevRaw : 0L;
        int closedPct  = t.getTargetLeadsClosed()  > 0 ? (int) Math.min(100, actualClosed * 100L / t.getTargetLeadsClosed())  : 0;
        int revPct     = t.getTargetRevenueLakhs()  > 0 ? (int) Math.min(100, actualRev    * 100L / t.getTargetRevenueLakhs()) : 0;
        return CposCrmSalesTargetDto.builder()
                .id(t.getId()).agentName(t.getAgentName())
                .periodMonth(t.getPeriodMonth()).periodYear(t.getPeriodYear())
                .targetLeadsClosed(t.getTargetLeadsClosed()).targetRevenueLakhs(t.getTargetRevenueLakhs())
                .actualLeadsClosed(actualClosed).actualRevenueLakhs(actualRev)
                .closedPct(closedPct).revenuePct(revPct).build();
    }

    @Transactional(readOnly = true)
    public List<CposCrmAnalyticsDto.LeaderboardEntryDto> getLeaderboard(AppUser user, int month, int year) {
        Long communityId = user.getCommunity().getId();
        List<Object[]> rows = leadRepo.leaderboardForMonth(communityId, month, year);
        List<CposCrmAnalyticsDto.LeaderboardEntryDto> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            String agent    = (String) row[0];
            long closed     = ((Number) row[1]).longValue();
            long revenue    = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long total      = leadRepo.countByCommunityIdAndDeletedFalse(communityId);
            result.add(CposCrmAnalyticsDto.LeaderboardEntryDto.builder()
                    .rank(i + 1).agent(agent).closedLeads(closed)
                    .revenueLakhs(revenue).totalLeads(total)
                    .score((int) Math.min(100, closed * 10 + revenue / 10))
                    .build());
        }
        return result;
    }

    // ── Bulk Email Campaign ───────────────────────────────────────────────

    @Transactional
    public CrmEmailCampaignDto sendBulkEmailCampaign(AppUser user, Map<String, Object> body) {
        if (!isCrmAdmin(user)) throw new ResourceNotFoundException("Admin only");
        Long communityId = user.getCommunity().getId();
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        String subject  = (String) body.getOrDefault("subject", "");
        String bodyHtml = (String) body.getOrDefault("bodyHtml", "");
        String filterStage  = (String) body.get("filterStage");
        String filterSource = (String) body.get("filterSource");

        List<CposCrmLead> leads = leadRepo.findForEmailCampaign(communityId,
                filterStage != null && !filterStage.isBlank() ? filterStage : null,
                filterSource != null && !filterSource.isBlank() ? filterSource : null);

        int sentCount = 0;
        String senderName = user.getFullName() != null ? user.getFullName() : user.getEmail();
        for (CposCrmLead lead : leads) {
            if (lead.getEmail() == null || lead.getEmail().isBlank()) continue;
            try {
                String personalised = bodyHtml.replace("{{name}}", lead.getLeadName())
                        .replace("{{phone}}", lead.getPhone() != null ? lead.getPhone() : "");
                emailService.send(EmailMessage.builder()
                        .to(lead.getEmail())
                        .subject(subject)
                        .htmlBody(personalised)
                        .build());
                sentCount++;
            } catch (Exception ignored) {}
        }

        CposCrmEmailCampaign campaign = CposCrmEmailCampaign.builder()
                .community(community)
                .subject(subject)
                .bodyHtml(bodyHtml)
                .filterStage(filterStage)
                .filterSource(filterSource)
                .sentCount(sentCount)
                .createdBy(senderName)
                .build();
        CposCrmEmailCampaign saved = emailCampaignRepo.save(campaign);
        return CrmEmailCampaignDto.builder()
                .id(saved.getId()).subject(saved.getSubject()).bodyHtml(saved.getBodyHtml())
                .filterStage(saved.getFilterStage()).filterSource(saved.getFilterSource())
                .sentCount(saved.getSentCount()).createdBy(saved.getCreatedBy()).sentAt(saved.getSentAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CrmEmailCampaignDto> getEmailCampaigns(AppUser user) {
        return emailCampaignRepo.findByCommunityIdOrderBySentAtDesc(user.getCommunity().getId())
                .stream().map(c -> CrmEmailCampaignDto.builder()
                        .id(c.getId()).subject(c.getSubject()).filterStage(c.getFilterStage())
                        .filterSource(c.getFilterSource()).sentCount(c.getSentCount())
                        .createdBy(c.getCreatedBy()).sentAt(c.getSentAt()).build())
                .collect(Collectors.toList());
    }

    // ── SMS ───────────────────────────────────────────────────────────────

    public void sendLeadSms(AppUser user, Long leadId, String message) {
        CposCrmLead lead = leadRepo.findByCommunityIdAndId(user.getCommunity().getId(), leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        if (lead.getPhone() == null || lead.getPhone().isBlank())
            throw new IllegalArgumentException("Lead has no phone number");
        smsProvider.send(SmsSendRequest.builder()
                .to(lead.getPhone()).body(message).build());
        CposCrmActivity activity = CposCrmActivity.builder()
                .lead(lead).activityType("NOTE")
                .note("SMS sent: " + message)
                .createdBy(user.getFullName() != null ? user.getFullName() : user.getEmail())
                .build();
        activityRepo.save(activity);
    }

    // ── Approval Workflows ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CposCrmLeadDto> getLeadsAwaitingApproval(AppUser user) {
        Long communityId = user.getCommunity().getId();
        return leadRepo.findByCommunityIdAndDeletedFalseOrderByCreatedAtDesc(communityId)
                .stream()
                .filter(l -> "PENDING_APPROVAL".equals(l.getApprovalStatus()))
                .map(this::toLeadDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CposCrmLeadDto requestApproval(AppUser user, Long leadId, String reason) {
        CposCrmLead lead = leadRepo.findByCommunityIdAndId(user.getCommunity().getId(), leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        lead.setApprovalStatus("PENDING_APPROVAL");
        lead.setApprovalReason(reason);
        leadRepo.save(lead);
        CposCrmActivity activity = CposCrmActivity.builder()
                .lead(lead).activityType("NOTE")
                .note("Approval requested: " + reason)
                .createdBy(user.getFullName() != null ? user.getFullName() : user.getEmail())
                .build();
        activityRepo.save(activity);
        // notify admins
        appUserRepo.findByCommunityIdAndIsActiveTrue(user.getCommunity().getId()).stream()
                .filter(u -> u.hasRole("ADMIN") || u.hasRole("MANAGER"))
                .forEach(admin -> {
                    try {
                        expoPushService.sendToUser(admin.getId(),
                                "Approval Required",
                                lead.getLeadName() + " needs sign-off before NEGOTIATION",
                                Map.of("type", "CRM_APPROVAL_REQUEST", "leadId", lead.getId()),
                                "default");
                    } catch (Exception ignored) {}
                });
        return toLeadDto(lead);
    }

    @Transactional
    public CposCrmLeadDto approveOrRejectLead(AppUser user, Long leadId, boolean approve, String comment) {
        if (!isCrmAdmin(user)) throw new ResourceNotFoundException("Admin only");
        CposCrmLead lead = leadRepo.findByCommunityIdAndId(user.getCommunity().getId(), leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        lead.setApprovalStatus(approve ? "APPROVED" : "REJECTED");
        lead.setApprovalComment(comment);
        if (approve) lead.setStage("NEGOTIATION");
        leadRepo.save(lead);
        CposCrmActivity activity = CposCrmActivity.builder()
                .lead(lead).activityType("NOTE")
                .note((approve ? "✓ Approved" : "✗ Rejected") + (comment != null ? ": " + comment : ""))
                .createdBy(user.getFullName() != null ? user.getFullName() : user.getEmail())
                .build();
        activityRepo.save(activity);
        return toLeadDto(lead);
    }

    // ── Email Drip Sequences ──────────────────────────────────────────────

    @Transactional
    public CposCrmDripSequenceDto createDripSequence(AppUser user, CposCrmDripSequenceDto req) {
        if (!isCrmAdmin(user)) throw new ResourceNotFoundException("Admin only");
        Long communityId = user.getCommunity().getId();
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));
        CposCrmDripSequence seq = CposCrmDripSequence.builder()
                .community(community).name(req.getName())
                .triggerStage(req.getTriggerStage()).active(true)
                .build();
        seq = dripSequenceRepo.save(seq);
        if (req.getSteps() != null) {
            CposCrmDripSequence finalSeq = seq;
            List<CposCrmDripStep> steps = req.getSteps().stream().map(s ->
                    CposCrmDripStep.builder()
                            .sequence(finalSeq).delayDays(s.getDelayDays())
                            .subject(s.getSubject()).bodyHtml(s.getBodyHtml())
                            .build()
            ).collect(Collectors.toList());
            dripStepRepo.saveAll(steps);
            seq.setSteps(steps);
        }
        return toDripDto(seq);
    }

    @Transactional(readOnly = true)
    public List<CposCrmDripSequenceDto> getDripSequences(AppUser user) {
        return dripSequenceRepo.findByCommunityIdOrderByNameAsc(user.getCommunity().getId())
                .stream().map(this::toDripDto).collect(Collectors.toList());
    }

    @Transactional
    public void deleteDripSequence(AppUser user, Long seqId) {
        if (!isCrmAdmin(user)) throw new ResourceNotFoundException("Admin only");
        CposCrmDripSequence seq = dripSequenceRepo.findByCommunityIdAndId(user.getCommunity().getId(), seqId)
                .orElseThrow(() -> new ResourceNotFoundException("Sequence not found"));
        dripSequenceRepo.delete(seq);
    }

    private CposCrmDripSequenceDto toDripDto(CposCrmDripSequence seq) {
        List<CposCrmDripSequenceDto.StepDto> steps = seq.getSteps() == null ? List.of() :
                seq.getSteps().stream().map(s -> CposCrmDripSequenceDto.StepDto.builder()
                        .id(s.getId()).delayDays(s.getDelayDays())
                        .subject(s.getSubject()).bodyHtml(s.getBodyHtml()).build())
                .collect(Collectors.toList());
        return CposCrmDripSequenceDto.builder()
                .id(seq.getId()).name(seq.getName())
                .triggerStage(seq.getTriggerStage()).active(seq.isActive())
                .steps(steps).build();
    }
}
