package com.manacommunity.api.scheduler;

import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.model.CposCrmDripSequence;
import com.manacommunity.api.model.CposCrmLead;
import com.manacommunity.api.repository.CposCrmDripSequenceRepository;
import com.manacommunity.api.repository.CposCrmLeadRepository;
import com.manacommunity.api.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrmDripScheduler {

    private final CommunityRepository communityRepo;
    private final CposCrmDripSequenceRepository dripSequenceRepo;
    private final CposCrmLeadRepository leadRepo;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendDripEmails() {
        communityRepo.findAll().forEach(community -> {
            Long communityId = community.getId();
            List<CposCrmLead> leads = leadRepo.findByCommunityIdAndDeletedFalseOrderByCreatedAtDesc(communityId);
            leads.stream()
                    .filter(l -> l.getEmail() != null && !l.getEmail().isBlank())
                    .forEach(lead -> {
                        List<CposCrmDripSequence> sequences =
                                dripSequenceRepo.findByCommunityIdAndTriggerStageAndActiveTrue(communityId, lead.getStage());
                        sequences.forEach(seq -> seq.getSteps().forEach(step -> {
                            long daysInStage = java.time.temporal.ChronoUnit.DAYS.between(
                                    lead.getStageChangedAt(), LocalDateTime.now());
                            if (daysInStage == step.getDelayDays()) {
                                try {
                                    String body = step.getBodyHtml()
                                            .replace("{{name}}", lead.getLeadName() != null ? lead.getLeadName() : "")
                                            .replace("{{phone}}", lead.getPhone() != null ? lead.getPhone() : "");
                                    emailService.send(EmailMessage.builder()
                                            .to(lead.getEmail())
                                            .subject(step.getSubject())
                                            .htmlBody(body)
                                            .build());
                                } catch (Exception e) {
                                    log.warn("Drip email failed for lead {}: {}", lead.getId(), e.getMessage());
                                }
                            }
                        }));
                    });
        });
    }
}
