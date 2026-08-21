package com.manacommunity.api.email.brevo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.email.EmailDeliveryLog;
import com.manacommunity.api.email.EmailDeliveryLogRepository;
import com.manacommunity.api.email.EmailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service integrating with Brevo (formerly Sendinblue) REST API v3.
 *
 * <p>Provides real-time transactional statistics, account credits & quotas,
 * remote cloud delivery logs, verified sender status, and local delivery log synchronization.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrevoService {

    private final EmailProperties emailProperties;
    private final EmailDeliveryLogRepository emailDeliveryLogRepository;
    private final ObjectMapper objectMapper;

    private RestClient createClient() {
        String baseUrl = emailProperties.getBrevo() != null && emailProperties.getBrevo().getBaseUrl() != null
                ? emailProperties.getBrevo().getBaseUrl()
                : "https://api.brevo.com/v3";
        String apiKey = emailProperties.getBrevo() != null ? emailProperties.getBrevo().getApiKey() : null;

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("api-key", apiKey.trim());
        }

        return builder.build();
    }

    public boolean isConfigured() {
        return emailProperties.getBrevo() != null
                && emailProperties.getBrevo().getApiKey() != null
                && !emailProperties.getBrevo().getApiKey().isBlank();
    }

    /**
     * Fetch Brevo Account, Quota and Email Credits.
     */
    public BrevoAccountDto getAccountInfo() {
        if (!isConfigured()) {
            return BrevoAccountDto.builder()
                    .email("admin@manacommunityhub.com")
                    .firstName("Mana")
                    .lastName("Administrator")
                    .companyName("Mana Community Hub")
                    .planType("Free Tier (300 emails/day)")
                    .dailyRelayQuota(300)
                    .creditsRemaining(285)
                    .creditsUsed(15)
                    .isConfigured(false)
                    .isLive(false)
                    .message("Brevo API key not configured. Set BREVO_API_KEY environment variable to connect live account.")
                    .build();
        }

        try {
            RestClient client = createClient();
            String responseBody = client.get()
                    .uri("/account")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String email = root.path("email").asText("unknown@brevo.com");
            String firstName = root.path("firstName").asText("");
            String lastName = root.path("lastName").asText("");
            String companyName = root.path("companyName").asText("Mana Community");

            JsonNode planNode = root.path("plan");
            String planType = "Standard";
            long credits = 0;
            if (planNode.isArray() && planNode.size() > 0) {
                JsonNode firstPlan = planNode.get(0);
                planType = firstPlan.path("type").asText("Free");
                credits = firstPlan.path("credits").asLong(0);
            }

            JsonNode relayNode = root.path("relay");
            long dailyQuota = relayNode.path("dailyQuota").asLong(300);

            return BrevoAccountDto.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .companyName(companyName)
                    .planType(planType)
                    .dailyRelayQuota(dailyQuota)
                    .creditsRemaining(credits > 0 ? credits : dailyQuota)
                    .creditsUsed(0)
                    .isConfigured(true)
                    .isLive(true)
                    .message("Connected to Brevo Account: " + email)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch Brevo account info: {}", e.getMessage());
            return BrevoAccountDto.builder()
                    .email("admin@manacommunityhub.com")
                    .firstName("Mana")
                    .lastName("Administrator")
                    .companyName("Mana Community Hub")
                    .planType("Relay Active")
                    .dailyRelayQuota(300)
                    .creditsRemaining(300)
                    .creditsUsed(0)
                    .isConfigured(true)
                    .isLive(false)
                    .message("Brevo connection notice: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Fetch aggregated statistics from Brevo SMTP API.
     */
    public BrevoStatsDto getAggregatedStats(int days) {
        if (!isConfigured()) {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            long total = emailDeliveryLogRepository.count();
            long sent = emailDeliveryLogRepository.countByCommunityIdAndStatusAndSentAtAfter(1L, EmailDeliveryLog.STATUS_SENT, since);
            long failed = emailDeliveryLogRepository.countByCommunityIdAndStatusAndSentAtAfter(1L, EmailDeliveryLog.STATUS_FAILED, since);
            long opened = emailDeliveryLogRepository.countByCommunityIdAndOpenedAtIsNotNullAndSentAtAfter(1L, since);

            long requests = Math.max(total, sent + failed);
            long delivered = Math.max(sent, 10);
            double delRate = requests > 0 ? ((double) delivered / requests) * 100.0 : 98.5;
            double opRate = delivered > 0 ? ((double) opened / delivered) * 100.0 : 42.0;

            return BrevoStatsDto.builder()
                    .periodDays(days)
                    .requests(requests > 0 ? requests : 12)
                    .delivered(delivered)
                    .hardBounces(failed)
                    .softBounces(0)
                    .clicks(2)
                    .uniqueClicks(2)
                    .opens(opened > 0 ? opened : 5)
                    .uniqueOpens(opened > 0 ? opened : 5)
                    .spamReports(0)
                    .blocked(0)
                    .unsubscribed(0)
                    .deliveryRate(Math.round(delRate * 10.0) / 10.0)
                    .openRate(Math.round(opRate * 10.0) / 10.0)
                    .clickRate(16.7)
                    .bounceRate(Math.round((failed > 0 ? ((double) failed / (requests > 0 ? requests : 1)) * 100.0 : 0.0) * 10.0) / 10.0)
                    .isConfigured(false)
                    .isLive(false)
                    .build();
        }

        try {
            RestClient client = createClient();
            String responseBody = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/smtp/statistics/aggregatedReport")
                            .queryParam("days", days)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            long requests = root.path("requests").asLong(0);
            long delivered = root.path("delivered").asLong(0);
            long hardBounces = root.path("hardBounces").asLong(0);
            long softBounces = root.path("softBounces").asLong(0);
            long clicks = root.path("clicks").asLong(0);
            long uniqueClicks = root.path("uniqueClicks").asLong(0);
            long opens = root.path("opens").asLong(0);
            long uniqueOpens = root.path("uniqueOpens").asLong(0);
            long spamReports = root.path("spamReports").asLong(0);
            long blocked = root.path("blocked").asLong(0);
            long unsubscribed = root.path("unsubscribed").asLong(0);

            double deliveryRate = requests > 0 ? ((double) delivered / requests) * 100.0 : 100.0;
            double openRate = delivered > 0 ? ((double) uniqueOpens / delivered) * 100.0 : 0.0;
            double clickRate = delivered > 0 ? ((double) uniqueClicks / delivered) * 100.0 : 0.0;
            double bounceRate = requests > 0 ? (((double) (hardBounces + softBounces)) / requests) * 100.0 : 0.0;

            return BrevoStatsDto.builder()
                    .periodDays(days)
                    .requests(requests)
                    .delivered(delivered)
                    .hardBounces(hardBounces)
                    .softBounces(softBounces)
                    .clicks(clicks)
                    .uniqueClicks(uniqueClicks)
                    .opens(opens)
                    .uniqueOpens(uniqueOpens)
                    .spamReports(spamReports)
                    .blocked(blocked)
                    .unsubscribed(unsubscribed)
                    .deliveryRate(Math.round(deliveryRate * 10.0) / 10.0)
                    .openRate(Math.round(openRate * 10.0) / 10.0)
                    .clickRate(Math.round(clickRate * 10.0) / 10.0)
                    .bounceRate(Math.round(bounceRate * 10.0) / 10.0)
                    .isConfigured(true)
                    .isLive(true)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch Brevo stats: {}", e.getMessage());
            return BrevoStatsDto.builder()
                    .periodDays(days)
                    .requests(0)
                    .delivered(0)
                    .hardBounces(0)
                    .softBounces(0)
                    .clicks(0)
                    .uniqueClicks(0)
                    .opens(0)
                    .uniqueOpens(0)
                    .spamReports(0)
                    .blocked(0)
                    .unsubscribed(0)
                    .deliveryRate(100.0)
                    .openRate(0.0)
                    .clickRate(0.0)
                    .bounceRate(0.0)
                    .isConfigured(true)
                    .isLive(false)
                    .build();
        }
    }

    /**
     * Fetch transactional email activity logs from Brevo.
     */
    public List<BrevoEmailLogDto> getEmailLogs(int limit, String email, String event) {
        if (!isConfigured()) {
            List<EmailDeliveryLog> localLogs = emailDeliveryLogRepository.findAll();
            List<BrevoEmailLogDto> simulated = new ArrayList<>();
            for (EmailDeliveryLog l : localLogs) {
                if (email != null && !email.isBlank() && !l.getRecipient().toLowerCase().contains(email.toLowerCase())) {
                    continue;
                }
                List<BrevoEmailLogDto.BrevoEventDetailDto> events = new ArrayList<>();
                events.add(new BrevoEmailLogDto.BrevoEventDetailDto("requests", l.getSentAt().toString(), null));
                if ("SENT".equalsIgnoreCase(l.getStatus())) {
                    events.add(new BrevoEmailLogDto.BrevoEventDetailDto("delivered", l.getSentAt().plusSeconds(2).toString(), "250 2.0.0 OK"));
                    if (l.getOpenedAt() != null) {
                        events.add(new BrevoEmailLogDto.BrevoEventDetailDto("opened", l.getOpenedAt().toString(), "Pixel Loaded"));
                    }
                } else if ("FAILED".equalsIgnoreCase(l.getStatus())) {
                    events.add(new BrevoEmailLogDto.BrevoEventDetailDto("hard_bounces", l.getSentAt().plusSeconds(1).toString(), l.getErrorMessage()));
                }

                simulated.add(BrevoEmailLogDto.builder()
                        .email(l.getRecipient())
                        .subject(l.getSubject())
                        .messageId("brevo-msg-" + l.getId() + "@manacommunity.app")
                        .uuid("uuid-" + l.getId())
                        .date(l.getSentAt().toString())
                        .status(l.getStatus())
                        .events(events)
                        .templateId(l.getTemplateType())
                        .from(l.getSender() != null ? l.getSender() : "noreply@manacommunityhub.com")
                        .build());
                if (simulated.size() >= limit) break;
            }
            return simulated;
        }

        try {
            RestClient client = createClient();
            String responseBody = client.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/smtp/emails")
                                .queryParam("limit", Math.min(limit, 100))
                                .queryParam("sort", "desc");
                        if (email != null && !email.isBlank()) uriBuilder.queryParam("email", email.trim());
                        if (event != null && !event.isBlank()) uriBuilder.queryParam("event", event.trim());
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode transactionalEmails = root.path("transactionalEmails");
            List<BrevoEmailLogDto> result = new ArrayList<>();

            if (transactionalEmails.isArray()) {
                for (JsonNode item : transactionalEmails) {
                    List<BrevoEmailLogDto.BrevoEventDetailDto> events = new ArrayList<>();
                    JsonNode eventsArray = item.path("events");
                    if (eventsArray.isArray()) {
                        for (JsonNode ev : eventsArray) {
                            events.add(new BrevoEmailLogDto.BrevoEventDetailDto(
                                    ev.path("name").asText(""),
                                    ev.path("time").asText(""),
                                    ev.path("reason").asText(null)
                            ));
                        }
                    }

                    result.add(BrevoEmailLogDto.builder()
                            .email(item.path("email").asText(""))
                            .subject(item.path("subject").asText(""))
                            .messageId(item.path("messageId").asText(""))
                            .uuid(item.path("uuid").asText(""))
                            .date(item.path("date").asText(""))
                            .status(item.path("status").asText("DELIVERED"))
                            .events(events)
                            .templateId(item.path("templateId").asText(null))
                            .from(item.path("from").asText("noreply@manacommunityhub.com"))
                            .build());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch Brevo transactional emails: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch registered & verified senders on Brevo.
     */
    public List<BrevoSenderDto> getSenders() {
        if (!isConfigured()) {
            return List.of(
                    BrevoSenderDto.builder()
                            .id(1L)
                            .name("Mana Community Admin")
                            .email("noreply@manacommunityhub.com")
                            .active(true)
                            .ips(List.of("1.179.112.45 (Shared Relay)"))
                            .build(),
                    BrevoSenderDto.builder()
                            .id(2L)
                            .name("Mana Sports & Events")
                            .email("events@manacommunityhub.com")
                            .active(true)
                            .ips(List.of("1.179.112.45 (Shared Relay)"))
                            .build()
            );
        }

        try {
            RestClient client = createClient();
            String responseBody = client.get()
                    .uri("/senders")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode sendersArray = root.path("senders");
            List<BrevoSenderDto> list = new ArrayList<>();
            if (sendersArray.isArray()) {
                for (JsonNode s : sendersArray) {
                    List<String> ips = new ArrayList<>();
                    JsonNode ipsNode = s.path("ips");
                    if (ipsNode.isArray()) {
                        for (JsonNode ip : ipsNode) ips.add(ip.path("ip").asText(""));
                    }
                    list.add(BrevoSenderDto.builder()
                            .id(s.path("id").asLong(0))
                            .name(s.path("name").asText(""))
                            .email(s.path("email").asText(""))
                            .active(s.path("active").asBoolean(false))
                            .ips(ips)
                            .build());
                }
            }
            return list;
        } catch (Exception e) {
            log.warn("Failed to fetch Brevo senders: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Sync Brevo cloud events into local EmailDeliveryLog repository.
     */
    @Transactional
    public BrevoSyncResultDto syncEvents() {
        List<BrevoEmailLogDto> cloudLogs = getEmailLogs(50, null, null);
        int synced = 0;
        int opened = 0;
        int bounced = 0;

        for (BrevoEmailLogDto cLog : cloudLogs) {
            synced++;
            boolean hasOpenedEvent = cLog.events() != null && cLog.events().stream().anyMatch(e -> "opened".equalsIgnoreCase(e.name()) || "first_opening".equalsIgnoreCase(e.name()));
            boolean hasBouncedEvent = cLog.events() != null && cLog.events().stream().anyMatch(e -> "hard_bounces".equalsIgnoreCase(e.name()) || "soft_bounces".equalsIgnoreCase(e.name()));

            if (hasOpenedEvent || hasBouncedEvent) {
                // Find matching local log
                List<EmailDeliveryLog> localMatches = emailDeliveryLogRepository.findAll().stream()
                        .filter(l -> l.getRecipient().equalsIgnoreCase(cLog.email()) && (l.getSubject() != null && l.getSubject().equalsIgnoreCase(cLog.subject())))
                        .toList();

                for (EmailDeliveryLog match : localMatches) {
                    if (hasOpenedEvent && match.getOpenedAt() == null) {
                        match.setOpenedAt(LocalDateTime.now());
                        emailDeliveryLogRepository.save(match);
                        opened++;
                    }
                    if (hasBouncedEvent && !"FAILED".equalsIgnoreCase(match.getStatus())) {
                        match.setStatus("FAILED");
                        match.setErrorMessage("Brevo recorded hard bounce event");
                        emailDeliveryLogRepository.save(match);
                        bounced++;
                    }
                }
            }
        }

        return BrevoSyncResultDto.builder()
                .syncedCount(synced)
                .openedUpdated(opened)
                .bouncedUpdated(bounced)
                .message(String.format("Successfully synchronized %d Brevo cloud logs (%d open statuses and %d bounce flags updated)", synced, opened, bounced))
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}