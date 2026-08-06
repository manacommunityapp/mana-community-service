package com.cpn.domain.communication.repository;

import com.cpn.domain.communication.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = :u1 AND m.recipientId = :u2) OR (m.senderId = :u2 AND m.recipientId = :u1) ORDER BY m.createdAt ASC")
    List<ChatMessage> findConversationBetween(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
