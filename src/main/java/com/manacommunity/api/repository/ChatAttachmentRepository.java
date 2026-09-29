package com.manacommunity.api.repository;

import com.manacommunity.api.model.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {

    List<ChatAttachment> findByMessageId(Long messageId);

    List<ChatAttachment> findByMessageIdIn(List<Long> messageIds);
}
