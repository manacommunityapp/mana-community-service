package com.manacommunity.api.email;

import com.manacommunity.api.dto.SportsTournamentAnnouncementRequest;
import com.manacommunity.api.model.*;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsTournamentAnnouncementService {

    private final EmailSupport support;
    private final EmailTemplateRenderer renderer;
    private final EmailService emailService;
    private final NotificationManagementService notificationService;
    private final AppUserRepository userRepo;
    private final SportsTournamentEmailService tournamentEmailService;

    public int announce(SportsTournament tournament, SportsTournamentAnnouncementRequest req) {
        Long communityId = tournament.getCommunity() != null ? tournament.getCommunity().getId() : null;
        if (communityId == null) {
            log.warn("SportsTournament {} has no community — cannot send announcement", tournament.getId());
            return 0;
        }

        List<AppUser> recipients = userRepo.findByCommunityIdAndIsActiveTrue(communityId);
        if (recipients.isEmpty()) return 0;

        EmailTemplate template = resolveTemplate(req.template());
        String subject = req.subject();

        int sent = 0;
        for (AppUser user : recipients) {
            try {
                String html;
                if (req.customHtml() != null && !req.customHtml().isBlank()) {
                    html = req.customHtml();
                } else {
                    // Single DTO drives the template — built once per recipient with
                    // the per-send custom message applied.
                    Map<String, Object> vars = support.baseVars(user.getFullName());
                    vars.put("email", tournamentEmailService.buildTournamentAnnouncementDTO(tournament, req.message()));
                    html = renderer.render(template, vars);
                }

                if (req.sendEmail()) {
                    emailService.send(new EmailMessage(
                            user.getEmail(), user.getFullName(), subject, html));
                }

                if (req.sendPush()) {
                    notificationService.createEmailNotification(
                            user.getId(),
                            template == EmailTemplate.TOURNAMENT_OPEN
                                    ? NotificationType.TOURNAMENT_OPEN
                                    : NotificationType.TOURNAMENT_ANNOUNCEMENT,
                            NotificationCategory.SPORTS,
                            subject,
                            req.message(),
                            support.props().getBaseUrl() + "/sports",
                            ReferenceType.TOURNAMENT,
                            tournament.getId(),
                            NotificationPriority.HIGH);
                }
                sent++;
            } catch (Exception e) {
                log.error("Failed to send announcement to user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("SportsTournament announcement sent to {}/{} recipients for tournament {}",
                sent, recipients.size(), tournament.getId());
        return sent;
    }

    private EmailTemplate resolveTemplate(String templateName) {
        if (templateName == null || templateName.isBlank()) return EmailTemplate.TOURNAMENT_ANNOUNCEMENT;
        try {
            return EmailTemplate.valueOf(templateName);
        } catch (IllegalArgumentException e) {
            return EmailTemplate.TOURNAMENT_ANNOUNCEMENT;
        }
    }

}
