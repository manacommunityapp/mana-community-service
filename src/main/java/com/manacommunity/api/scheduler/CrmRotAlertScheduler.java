package com.manacommunity.api.scheduler;

import com.manacommunity.api.model.CposCrmLead;
import com.manacommunity.api.repository.CposCrmLeadRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Daily check for leads that haven't progressed in too long.
 * Threshold per stage — if a lead has had no update within the window it is "rotting"
 * and the assigned agent gets a push reminder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmRotAlertScheduler {

    private static final Map<String, Integer> STAGE_STALE_DAYS = Map.of(
            "NEW",         3,
            "CONTACTED",   5,
            "QUALIFIED",   7,
            "SITE_VISIT",  4,
            "NEGOTIATION", 14
    );

    private final CposCrmLeadRepository leadRepo;
    private final CommunityRepository communityRepo;
    private final AppUserRepository appUserRepo;
    private final ExpoPushService expoPushService;

    @Scheduled(cron = "0 0 8 * * *")   // 08:00 daily
    @Transactional(readOnly = true)
    public void alertStaleLeads() {
        communityRepo.findAll().forEach(community -> {
            for (Map.Entry<String, Integer> entry : STAGE_STALE_DAYS.entrySet()) {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(entry.getValue());
                List<CposCrmLead> stale = leadRepo.findStaleLeads(community.getId(), cutoff)
                        .stream().filter(l -> entry.getKey().equals(l.getStage())).toList();
                if (stale.isEmpty()) continue;
                log.info("CRM rot-alert: {} stale {} lead(s) in community {}", stale.size(), entry.getKey(), community.getId());
                for (CposCrmLead lead : stale) {
                    if (lead.getAssignedTo() == null) continue;
                    appUserRepo.findFirstByCommunityIdAndFullNameIgnoreCase(community.getId(), lead.getAssignedTo())
                            .ifPresent(agent -> expoPushService.sendToUser(
                                    agent.getId(),
                                    "⚠️ Lead going cold",
                                    lead.getLeadName() + " has had no activity for " + entry.getValue() + "+ days",
                                    Map.of("type", "CRM_ROT_ALERT", "leadId", lead.getId()),
                                    "default"));
                }
            }
        });
    }
}
