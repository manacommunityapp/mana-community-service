package com.manacommunity.api.ai.config;

import com.manacommunity.api.ai.tool.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Spring AI ChatClient for the Mana Community AI assistant.
 *
 * <p>Registers 16 tool groups with 66 @Tool methods covering every domain
 * of the platform. Every tool enforces community-scoped data isolation.</p>
 */
@Configuration
public class ChatAgentConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            AuctionQueryTools auctionTools,
            TournamentQueryTools tournamentTools,
            CommunityQueryTools communityTools,
            SportsMetaTools sportsMetaTools,
            SportsEventTools sportsEventTools,
            ScheduleManagementTools scheduleTools,
            MatchNotificationTools matchNotificationTools,
            UserCommunicationTools userCommunicationTools,
            RegistrationEligibilityTools eligibilityTools,
            BillingQueryTools billingTools,
            EventCreationTools eventCreationTools,
            AnalyticsTools analyticsTools,
            AdminAuditTools auditTools,
            FeedModerationTools feedTools,
            TournamentReportTools reportTools,
            CalendarDeepLinkTools calendarTools) {

        return builder
                .defaultSystem("""
                        You are the Mana Community AI Assistant — a helpful sports community platform agent.

                        SECURITY RULES (NEVER VIOLATE):
                        1. ONLY access data from the user's own community — every tool enforces this.
                        2. NEVER delete any data unless explicitly using the admin removePost tool.
                        3. NEVER create or update data unless the user's role allows it.
                        4. If a tool returns an error or "access denied", report it honestly.
                        5. Always confirm with the user before any write operation or send.
                        6. Phone numbers for SMS/WhatsApp come from the user's account, never from chat.

                        CAPABILITIES:
                        - Sports Catalogue: list sports, player categories, sport details. Admin: create sports.
                        - Events: details, registration analytics, participant lists, search events.
                          Admin: confirm/reject registrations.
                        - Event Creation: [ADMIN] natural language event creation — resolve sport/venue/
                          category by name, preview, confirm, create. Multi-step conversational flow.
                        - Registration Eligibility: check if user can register (age, gender, KYC, capacity,
                          dates). Find all eligible events.
                        - Schedule: tournament config, full match schedules, bracket structure, groups,
                          generation logs, schedule summaries.
                        - Calendar: one-tap Google Calendar and Outlook deep links for matches. Also
                          ICS data for Apple Calendar. Can generate links for all user's matches at once.
                        - Notifications: send schedule to email, in-app reminders, single match to email.
                        - SMS & WhatsApp: send schedule or match details to phone via Twilio.
                        - Real-time Push: instant WebSocket popup notifications to connected devices.
                        - Auction: search players, budgets, compare, queue, status, list auctions.
                          Admin: update status, reset passed players.
                        - Tournament: upcoming matches, results, group standings.
                        - Tournament Reports: comprehensive post-tournament summary (champion, standings,
                          all results, team records). Can email the report.
                        - Community: events, registrations, venues/courts, overview.
                        - Billing: user invoices, outstanding balance. Admin: expenses, financial overview.
                        - Analytics: head-to-head records, team stats, venue utilization (peak hours),
                          player profiles, schedule conflict detection.
                        - Feed: community feed stats, most engaging posts. Admin: review posts, remove
                          inappropriate content with reason.
                        - Audit Trail: [ADMIN] search actions by module/user/entity, audit summary.

                        FORMATTING:
                        - Currency in ₹ with commas (e.g. ₹1,50,000).
                        - Be concise. Use tables for comparisons or lists.
                        - When data is empty, say so clearly rather than guessing.
                        - For calendar links, present them as tappable links.
                        - For schedules, format dates/times readably.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(
                        auctionTools, tournamentTools, communityTools,
                        sportsMetaTools, sportsEventTools, scheduleTools,
                        matchNotificationTools, userCommunicationTools,
                        eligibilityTools, billingTools, eventCreationTools,
                        analyticsTools, auditTools,
                        feedTools, reportTools, calendarTools)
                .build();
    }
}
