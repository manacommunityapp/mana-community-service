package com.manacommunity.api.ai.service;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.model.Role;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import com.manacommunity.api.ai.dto.AiChatRequest;
import com.manacommunity.api.ai.dto.AiChatResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.AuctionTeam;
import com.manacommunity.api.repository.AuctionTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Core service that handles AI chat interactions.
 *
 * <p>For each request it:</p>
 * <ol>
 *   <li>Resolves the user's team ownership (if any) from the database</li>
 *   <li>Sets up the {@link AgentSecurityContext} so all @Tool methods can access
 *       the validated user identity without re-querying</li>
 *   <li>Builds a per-request system prompt with the user's role, name, and permissions</li>
 *   <li>Calls the Spring AI {@link ChatClient} which invokes tools as needed</li>
 *   <li>Clears the security context in a finally block</li>
 * </ol>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AiChatAgentService {

    private final ChatClient chatClient;
    private final AuctionTeamRepository auctionTeamRepository;

    /**
     * Synchronous chat — returns the full response once the model finishes.
     */
    public AiChatResponse chat(AiChatRequest request, AppUser user) {
        log.info("AI chat: user={}, community={}, role={}",
                user.getId(), user.getCommunity().getId(), user.getRole());

        String conversationId = buildConversationId(user, request.conversationId());
        UserContext ctx = buildUserContext(user);

        AgentSecurityContext.set(ctx);
        try {
            String reply = chatClient.prompt()
                    .system(s -> s.text(buildSystemPrompt(ctx, request.auctionConfigId())))
                    .user(request.message())
                    .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                    .call()
                    .content();

            return new AiChatResponse(
                    request.conversationId(),
                    reply != null ? reply : "I couldn't generate a response. Please try again.");
        } catch (Exception e) {
            log.error("AI chat error for user={}: {}", user.getId(), e.getMessage(), e);
            return new AiChatResponse(
                    request.conversationId(),
                    "I encountered an error processing your request. Please try again.");
        } finally {
            AgentSecurityContext.clear();
        }
    }

    /**
     * Streaming chat — returns a Flux of text tokens for SSE.
     */
    public Flux<String> chatStream(AiChatRequest request, AppUser user) {
        log.info("AI chat stream: user={}, community={}", user.getId(), user.getCommunity().getId());

        String conversationId = buildConversationId(user, request.conversationId());
        UserContext ctx = buildUserContext(user);

        AgentSecurityContext.set(ctx);
        return chatClient.prompt()
                .system(s -> s.text(buildSystemPrompt(ctx, request.auctionConfigId())))
                .user(request.message())
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .stream()
                .content()
                .doFinally(signal -> AgentSecurityContext.clear());
    }

    // ── Private helpers ────────────────────────────────────────────────

    private UserContext buildUserContext(AppUser user) {
        Long communityId = user.getCommunity().getId();

        // Resolve team ownership — check if user owns any auction team in their community
        Long teamId = resolveTeamId(user.getId());

        boolean kycVerified = "VERIFIED".equalsIgnoreCase(user.getKycStatus());

        return new UserContext(
                user.getId(),
                communityId,
                user.getRole(),
                teamId,
                user.getFullName(),
                kycVerified);
    }

    private Long resolveTeamId(Long userId) {
        List<AuctionTeam> ownedTeams = auctionTeamRepository
                .findByOwnerUserIdOrCaptainUserId(userId, userId);
        return ownedTeams.isEmpty() ? null : ownedTeams.get(0).getId();
    }

    private String buildConversationId(AppUser user, Long requestConversationId) {
        if (requestConversationId != null) {
            return "user-" + user.getId() + "-conv-" + requestConversationId;
        }
        return "user-" + user.getId() + "-" + System.currentTimeMillis();
    }

    private String buildSystemPrompt(UserContext ctx, Long auctionConfigId) {
        StringBuilder sb = new StringBuilder();
        sb.append("CONTEXT — enforced by every tool, not just advisory:\n");
        sb.append("  Community ID: %d (all queries are scoped to this community)\n"
                .formatted(ctx.communityId()));
        sb.append("  User: %s (ID: %d)\n".formatted(
                ctx.fullName() != null ? ctx.fullName() : "User", ctx.userId()));
        sb.append("  Role: %s\n".formatted(ctx.role()));

        if (ctx.isSuperAdmin()) {
            sb.append("  Permissions: Full read + write access. Can create sports, confirm/reject ");
            sb.append("registrations, update auction status, and reset passed players (with confirmation). ");
            sb.append("Cannot delete anything.\n");
        } else if (ctx.isAdmin()) {
            sb.append("  Permissions: Full read access. Can create sports, confirm/reject registrations ");
            sb.append("within community. Cannot modify auctions.\n");
        } else {
            sb.append("  Permissions: Read-only access to community data.\n");
        }

        if (ctx.teamId() != null) {
            sb.append("  Team ownership: Owns team ID %d\n".formatted(ctx.teamId()));
        }

        if (auctionConfigId != null) {
            sb.append("  Active auction context: ID %d — pass this to auction tools.\n"
                    .formatted(auctionConfigId));
        } else {
            sb.append("  No auction selected — use listMyCommunityAuctions to find available ones.\n");
        }

        sb.append("  Available domains: Sports catalogue, Events & registrations, ");
        sb.append("Schedules & brackets, Auctions, Tournaments, Community & venues.\n");

        return sb.toString();
    }
}
