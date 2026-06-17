package com.manacommunity.api.repository;

import com.manacommunity.api.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Unread count for a user: messages created after their read watermark that
     * they did not send themselves (system messages, sender NULL, also count).
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.conversation.id = :conversationId
              AND m.createdAt > :since
              AND (m.sender IS NULL OR m.sender.id <> :userId)
            """)
    long countUnread(@Param("conversationId") Long conversationId,
                     @Param("since") LocalDateTime since,
                     @Param("userId") Long userId);
}
