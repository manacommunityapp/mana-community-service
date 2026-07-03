package com.manacommunity.api.service;

import com.manacommunity.api.dto.chat.ChatContactResponse;
import com.manacommunity.api.dto.chat.ChatMessageResponse;
import com.manacommunity.api.dto.chat.ConversationResponse;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.ChatMessage;
import com.manacommunity.api.model.Conversation;
import com.manacommunity.api.model.ConversationParticipant;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.repository.ChatMessageRepository;
import com.manacommunity.api.repository.ConversationParticipantRepository;
import com.manacommunity.api.repository.ConversationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REST-backed 1:1 chat. Conversations are scoped to participants; every read
 * verifies the caller is a member before returning anything.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final AppUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    // ── Conversations list ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(AppUser currentUser) {
        List<ConversationParticipant> memberships =
                participantRepository.findByUserIdOrderByConversationLastMessageAtDesc(currentUser.getId());
        List<ConversationResponse> result = new ArrayList<>();
        for (ConversationParticipant membership : memberships) {
            result.add(toConversationResponse(membership, currentUser.getId()));
        }
        return result;
    }

    // ── Find-or-create a 1:1 thread ───────────────────────────────────────

    @Transactional
    public ConversationResponse startDirect(AppUser currentUser, Long otherUserId) {
        if (otherUserId == null || otherUserId.equals(currentUser.getId())) {
            throw new InvalidInputException("A valid other user is required to start a conversation.");
        }
        AppUser other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", String.valueOf(otherUserId)));

        List<Long> existing = conversationRepository.findDirectConversationIds(currentUser.getId(), otherUserId);
        Conversation conversation;
        if (!existing.isEmpty()) {
            conversation = conversationRepository.findById(existing.get(0))
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", existing.get(0)));
        } else {
            conversation = conversationRepository.save(Conversation.builder()
                    .type("DIRECT")
                    .community(currentUser.getCommunity())
                    .build());
            participantRepository.save(ConversationParticipant.builder()
                    .conversation(conversation).user(currentUser).build());
            participantRepository.save(ConversationParticipant.builder()
                    .conversation(conversation).user(other).build());
        }

        ConversationParticipant membership = participantRepository
                .findByConversationIdAndUserId(conversation.getId(), currentUser.getId())
                .orElseThrow(() -> new UnauthorizedActionException("open conversation " + conversation.getId()));
        return toConversationResponse(membership, currentUser.getId());
    }

    // ── Messages ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(AppUser currentUser, Long conversationId) {
        requireMembership(conversationId, currentUser.getId());
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(AppUser currentUser, Long conversationId, String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidInputException("Message content must not be empty.");
        }
        ConversationParticipant membership = requireMembership(conversationId, currentUser.getId());
        Conversation conversation = membership.getConversation();

        ChatMessage message = messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .sender(currentUser)
                .type("TEXT")
                .content(content.trim())
                .build());

        conversation.setLastMessage(message.getContent());
        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        // The sender has implicitly read their own message.
        membership.setLastReadAt(message.getCreatedAt());
        participantRepository.save(membership);

        ChatMessageResponse response = toMessageResponse(message);

        // Metric: count every successfully-persisted chat message. Tagged by
        // conversation type so DIRECT vs GROUP traffic is separable in Grafana.
        meterRegistry.counter("chat.messages.sent", "type", conversation.getType()).increment();

        // ── Real-time push ────────────────────────────────────────────────
        // 1. Everyone viewing this thread gets the message instantly.
        messagingTemplate.convertAndSend("/topic/conversation/" + conversation.getId(), response);
        // 2. Each participant's client updates its conversation-list row in place
        //    (last message + unread badge) — no refetch needed. The payload
        //    carries everything the list needs; senderId lets each client decide
        //    whether to bump its own unread badge.
        ChatConversationEvent event = new ChatConversationEvent(
                conversation.getId(),
                message.getContent(),
                message.getCreatedAt(),
                currentUser.getId());
        for (ConversationParticipant p : participantRepository.findByConversationId(conversation.getId())) {
            messagingTemplate.convertAndSend("/topic/chat-user/" + p.getUser().getId(), event);
        }

        return response;
    }

    /** Conversation-list update pushed to each participant (replaces a client-side refetch). */
    public record ChatConversationEvent(Long conversationId, String lastMessage,
                                        LocalDateTime lastMessageAt, Long senderId) {}

    @Transactional
    public void markRead(AppUser currentUser, Long conversationId) {
        ConversationParticipant membership = requireMembership(conversationId, currentUser.getId());
        membership.setLastReadAt(LocalDateTime.now());
        participantRepository.save(membership);
    }

    // ── Contacts to start a chat with (community members) ─────────────────

    @Transactional(readOnly = true)
    public List<ChatContactResponse> getContacts(AppUser currentUser) {
        if (currentUser.getCommunity() == null) {
            return List.of();
        }
        return userRepository.findByCommunityId(currentUser.getCommunity().getId()).stream()
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .map(this::toContactResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ConversationParticipant requireMembership(Long conversationId, Long userId) {
        return participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new UnauthorizedActionException("access conversation " + conversationId));
    }

    private ConversationResponse toConversationResponse(ConversationParticipant membership, Long currentUserId) {
        Conversation conversation = membership.getConversation();
        boolean isGroup = "GROUP".equalsIgnoreCase(conversation.getType());

        ChatContactResponse contact = null;
        if (!isGroup) {
            contact = participantRepository.findByConversationId(conversation.getId()).stream()
                    .map(ConversationParticipant::getUser)
                    .filter(u -> !u.getId().equals(currentUserId))
                    .findFirst()
                    .map(this::toContactResponse)
                    .orElse(null);
        }

        LocalDateTime since = membership.getLastReadAt() == null ? EPOCH : membership.getLastReadAt();
        long unread = messageRepository.countUnread(conversation.getId(), since, currentUserId);

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getTitle(),
                isGroup,
                contact,
                conversation.getLastMessage(),
                conversation.getLastMessageAt(),
                unread
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        AppUser sender = message.getSender();
        return new ChatMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                sender != null ? sender.getId() : null,
                sender != null ? sender.getFullName() : null,
                message.getType(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private ChatContactResponse toContactResponse(AppUser user) {
        return new ChatContactResponse(
                user.getId(),
                user.getFullName(),
                mapRole(user.getRole()),
                getInitials(user.getFullName()),
                false, // presence is not tracked yet
                "VERIFIED".equalsIgnoreCase(user.getKycStatus())
        );
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String mapRole(String rawRole) {
        if ("ADMIN".equalsIgnoreCase(rawRole)) return "Admin";
        return "Verified Member";
    }
}
