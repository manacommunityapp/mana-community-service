package com.manacommunity.api.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published by ChatService (or ChatController) after a message is
 * successfully persisted. The push listener picks this up and notifies
 * all conversation participants who are not the sender.
 *
 * Usage in ChatServiceImpl:
 * <pre>
 *   &#64;Autowired ApplicationEventPublisher events;
 *   // after saving the message:
 *   events.publishEvent(new ChatMessageSentEvent(this,
 *       savedMessage.getId(),
 *       conversationId,
 *       sender.getId(),
 *       sender.getFullName(),
 *       content,
 *       recipientIds));
 * </pre>
 */
@Getter
public class ChatMessageSentEvent extends ApplicationEvent {

    private final Long       messageId;
    private final Long       conversationId;
    private final Long       senderId;
    private final String     senderName;
    /** Truncated preview — never log full content (privacy). */
    private final String     contentPreview;
    /** All participant IDs except the sender. */
    private final java.util.List<Long> recipientIds;

    public ChatMessageSentEvent(Object source,
                                Long messageId,
                                Long conversationId,
                                Long senderId,
                                String senderName,
                                String rawContent,
                                java.util.List<Long> recipientIds) {
        super(source);
        this.messageId      = messageId;
        this.conversationId = conversationId;
        this.senderId       = senderId;
        this.senderName     = senderName;
        // Truncate at 80 chars for the push preview
        this.contentPreview = rawContent != null && rawContent.length() > 80
                ? rawContent.substring(0, 77) + "…"
                : rawContent;
        this.recipientIds = recipientIds != null ? recipientIds : java.util.List.of();
    }
}
