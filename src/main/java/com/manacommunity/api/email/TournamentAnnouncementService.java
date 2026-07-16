package com.manacommunity.api.email;

import com.manacommunity.api.dto.TournamentAnnouncementRequest;
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
public class TournamentAnnouncementService {

    private final EmailSupport support;
    private final EmailTemplateRenderer renderer;
    private final EmailService emailService;
    private final NotificationManagementService notificationService;
    private final AppUserRepository userRepo;

    public int announce(Tournament tournament, TournamentAnnouncementRequest req) {
        Long communityId = tournament.getCommunity() != null ? tournament.getCommunity().getId() : null;
        if (communityId == null) {
            log.warn("Tournament {} has no community — cannot send announcement", tournament.getId());
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
                    html = renderer.render(template, buildVars(tournament, user, req));
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

        log.info("Tournament announcement sent to {}/{} recipients for tournament {}",
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

    private Map<String, Object> buildVars(Tournament t, AppUser user, TournamentAnnouncementRequest req) {
        Map<String, Object> vars = support.baseVars(user.getFullName());
        vars.put("tournamentName", t.getName());
        vars.put("subject", req.subject());
        vars.put("customMessage", req.message());
        vars.put("eventDate", support.formatDate(t.getEventDateStart()));
        vars.put("registrationDeadline", support.formatDate(t.getRegistrationDateEnd()));
        vars.put("contactName", t.getContactName());
        vars.put("contactNumber", t.getContactNumber());
        vars.put("bannerImage", t.getBannerImage());
        vars.put("actionUrl", support.props().getBaseUrl() + "/sports");

        List<SportsEvent> events = t.getSportsEvents();
        if (events != null && !events.isEmpty()) {
            String venueName = events.stream()
                    .filter(e -> e.getVenue() != null)
                    .map(e -> e.getVenue().getName())
                    .findFirst().orElse(null);
            vars.put("venueName", venueName);

            List<Map<String, String>> eventRows = new ArrayList<>();
            for (SportsEvent ev : events) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("sportName", ev.getSport() != null ? ev.getSport().getName() : ev.getName());
                row.put("eventName", ev.getName());
                row.put("icon", ev.getIcon());
                row.put("ageRange", ev.getMinAge() + "–" + ev.getMaxAge());
                row.put("gender", ev.getGender() != null ? support.prettify(ev.getGender()) : "All");
                eventRows.add(row);
            }
            vars.put("sportsEvents", eventRows);
        }
        return vars;
    }
}
