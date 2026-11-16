package com.manacommunity.api.security;

import com.manacommunity.api.repository.ConversationParticipantRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyCircleRepository;
import com.manacommunity.api.socialcircle.repository.KittyPartyGameRoomRepository;
import com.manacommunity.api.sports.repository.SportsTournamentConfigRepository;
import com.manacommunity.api.sports.repository.SportsTournamentMatchRepository;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authorizes STOMP SUBSCRIBE frames against the subscribing user's verified memberships.
 *
 * <p>Every topic that carries a tenant or resource ID (community, conversation, match,
 * auction, user, emergency, noticeboard, kitty game) is checked against the authenticated
 * principal via {@link CommunityMembershipService}. An unauthorized subscribe is silently
 * dropped (returns {@code null}), which the STOMP spec treats as a rejected subscription.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompSubscriptionInterceptor implements ChannelInterceptor {

    private static final Pattern CONVERSATION_TOPIC = Pattern.compile("^/topic/conversation/(\\d+)$");
    private static final Pattern CHAT_USER_TOPIC    = Pattern.compile("^/topic/chat-user/(\\d+)$");
    private static final Pattern NOTIFICATION_TOPIC  = Pattern.compile("^/topic/notifications/(\\d+)$");
    private static final Pattern AUCTION_TOPIC       = Pattern.compile("^/topic/auction/(\\d+)$");
    private static final Pattern MATCH_TOPIC         = Pattern.compile("^/topic/match/(\\d+)(?:/.*)?$");
    private static final Pattern KITTY_GAME_TOPIC    = Pattern.compile("^/topic/kitty-games/(\\d+)$");
    private static final Pattern COMMUNITY_TOPIC     = Pattern.compile("^/topic/community/(\\d+)(?:/.*)?$");
    private static final Pattern BROADCAST_TOPIC     = Pattern.compile("^/topic/broadcast/(\\d+)(?:/.*)?$");
    private static final Pattern EMERGENCY_TOPIC     = Pattern.compile("^/topic/emergency/(\\d+)(?:/.*)?$");
    private static final Pattern NOTICEBOARD_TOPIC   = Pattern.compile("^/topic/noticeboard/(\\d+)(?:/.*)?$");

    private final AppUserRepository userRepository;
    private final ConversationParticipantRepository participantRepository;
    private final SportsTournamentConfigRepository configRepository;
    private final SportsTournamentMatchRepository matchRepository;
    private final KittyPartyGameRoomRepository gameRoomRepository;
    private final CommunityMembershipService membershipService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null) return message;

        Long userId = extractUserId(accessor);
        if (userId == null) {
            log.warn("STOMP SUBSCRIBE rejected: no authenticated user for {}", destination);
            return null;
        }

        if (!isAuthorized(userId, destination)) {
            log.warn("STOMP SUBSCRIBE rejected: user={} destination={}", userId, destination);
            return null;
        }

        return message;
    }

    private boolean isAuthorized(Long userId, String destination) {
        Matcher m;

        m = CHAT_USER_TOPIC.matcher(destination);
        if (m.matches()) {
            long targetUserId = Long.parseLong(m.group(1));
            return userId.equals(targetUserId);
        }

        m = NOTIFICATION_TOPIC.matcher(destination);
        if (m.matches()) {
            long targetUserId = Long.parseLong(m.group(1));
            return userId.equals(targetUserId);
        }

        m = CONVERSATION_TOPIC.matcher(destination);
        if (m.matches()) {
            long conversationId = Long.parseLong(m.group(1));
            return participantRepository.findByConversationIdAndUserId(conversationId, userId)
                    .isPresent();
        }

        m = COMMUNITY_TOPIC.matcher(destination);
        if (m.matches()) {
            long communityId = Long.parseLong(m.group(1));
            return membershipService.isUserAuthorizedForCommunity(userId, communityId);
        }

        m = BROADCAST_TOPIC.matcher(destination);
        if (m.matches()) {
            long communityId = Long.parseLong(m.group(1));
            return membershipService.isUserAuthorizedForCommunity(userId, communityId);
        }

        m = EMERGENCY_TOPIC.matcher(destination);
        if (m.matches()) {
            long communityId = Long.parseLong(m.group(1));
            return membershipService.isUserAuthorizedForCommunity(userId, communityId);
        }

        m = NOTICEBOARD_TOPIC.matcher(destination);
        if (m.matches()) {
            long communityId = Long.parseLong(m.group(1));
            return membershipService.isUserAuthorizedForCommunity(userId, communityId);
        }

        m = AUCTION_TOPIC.matcher(destination);
        if (m.matches()) {
            long configId = Long.parseLong(m.group(1));
            return isUserInResourceCommunity(userId, configId, ResourceType.AUCTION_CONFIG);
        }

        m = MATCH_TOPIC.matcher(destination);
        if (m.matches()) {
            long matchId = Long.parseLong(m.group(1));
            return isUserInResourceCommunity(userId, matchId, ResourceType.MATCH);
        }

        m = KITTY_GAME_TOPIC.matcher(destination);
        if (m.matches()) {
            long gameRoomId = Long.parseLong(m.group(1));
            return isUserInResourceCommunity(userId, gameRoomId, ResourceType.KITTY_GAME);
        }

        // Default-deny for unrecognized topics under /topic/
        if (destination.startsWith("/topic/")) {
            log.warn("STOMP SUBSCRIBE rejected: unmapped topic {}", destination);
            return false;
        }

        return true;
    }

    private boolean isUserInResourceCommunity(Long userId, long resourceId, ResourceType type) {
        Long resourceCommunityId = switch (type) {
            case AUCTION_CONFIG -> configRepository.findById(resourceId)
                    .map(c -> c.getCommunity() != null ? c.getCommunity().getId() : null)
                    .orElse(null);
            case MATCH -> matchRepository.findById(resourceId)
                    .map(m -> m.getCommunity() != null ? m.getCommunity().getId() : null)
                    .orElse(null);
            case KITTY_GAME -> gameRoomRepository.findById(resourceId)
                    .map(gr -> gr.getCircle() != null && gr.getCircle().getCommunity() != null
                            ? gr.getCircle().getCommunity().getId()
                            : null)
                    .orElse(null);
        };
        if (resourceCommunityId == null) return false;

        return membershipService.isUserAuthorizedForCommunity(userId, resourceCommunityId);
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) return null;
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object p = auth.getPrincipal();
            if (p instanceof String s) {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private enum ResourceType {
        AUCTION_CONFIG, MATCH, KITTY_GAME
    }
}
