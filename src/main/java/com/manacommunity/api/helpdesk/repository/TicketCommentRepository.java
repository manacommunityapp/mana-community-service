package com.manacommunity.api.helpdesk.repository;

import com.manacommunity.api.helpdesk.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
}
