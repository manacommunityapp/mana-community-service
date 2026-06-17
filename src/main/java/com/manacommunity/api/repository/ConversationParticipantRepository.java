package com.manacommunity.api.repository;

import com.manacommunity.api.model.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    /** All conversations the user belongs to, newest activity first. */
    List<ConversationParticipant> findByUserIdOrderByConversationLastMessageAtDesc(Long userId);

    /** The caller's membership row in a conversation (authorisation + read state). */
    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    /** Everyone in a conversation (used to resolve the "other" party for DIRECT chats). */
    List<ConversationParticipant> findByConversationId(Long conversationId);
}
