package com.manacommunity.api.scheduler;

import com.manacommunity.api.model.CposCrmLead;
import com.manacommunity.api.repository.CposCrmLeadRepository;
import com.manacommunity.api.service.ExpoPushService;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fires push notifications for CRM follow-ups that become due within
 * each 15-minute scheduling window. The window approach (from → to) means
 * each follow-up triggers at most one notification per 15-min period.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmFollowUpScheduler {

    private final CposCrmLeadRepository leadRepo;
    private final AppUserRepository appUserRepo;
    private final ExpoPushService expoPushService;

    @Scheduled(fixedRate = 900_000)  // every 15 minutes
    @Transactional(readOnly = true)
    public void fireFollowUpReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusMinutes(15);

        List<CposCrmLead> due = leadRepo.findDueForNotification(from, now);
        if (due.isEmpty()) return;

        log.info("CRM follow-up scheduler: {} lead(s) due in window [{} → {}]", due.size(), from, now);

        for (CposCrmLead lead : due) {
            if (lead.getAssignedTo() == null) continue;
            Long communityId = lead.getCommunity().getId();
            appUserRepo.findFirstByCommunityIdAndFullNameIgnoreCase(communityId, lead.getAssignedTo())
                    .ifPresent(agent -> expoPushService.notifyCrmFollowUpDue(
                            agent.getId(),
                            lead.getLeadName(),
                            lead.getFollowUpNote(),
                            lead.getId()));
        }
    }
}
