package com.manacommunity.api.repository;

import com.manacommunity.api.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Ids of DIRECT conversations that contain BOTH users — used to find an
     * existing 1:1 thread before creating a new one. A unique constraint keeps
     * one participant row per (conversation,user), so a count of 2 means both
     * users are present.
     */
    @Query("""
            SELECT p.conversation.id FROM ConversationParticipant p
            WHERE p.conversation.type = 'DIRECT' AND p.user.id IN (:userA, :userB)
            GROUP BY p.conversation.id
            HAVING COUNT(p.user.id) = 2
            """)
    List<Long> findDirectConversationIds(@Param("userA") Long userA, @Param("userB") Long userB);
}
