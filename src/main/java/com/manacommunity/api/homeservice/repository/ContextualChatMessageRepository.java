package com.manacommunity.api.homeservice.repository;

import com.manacommunity.api.homeservice.model.entity.ContextualChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository("homeServiceContextualChatMessageRepository")
public interface ContextualChatMessageRepository extends JpaRepository<ContextualChatMessageEntity, String> {
    List<ContextualChatMessageEntity> findByBookingIdOrderByCreatedAtAsc(String bookingId);
    List<ContextualChatMessageEntity> findByWorkerIdAndResidentUserIdOrderByCreatedAtAsc(String workerId, String residentUserId);
}
