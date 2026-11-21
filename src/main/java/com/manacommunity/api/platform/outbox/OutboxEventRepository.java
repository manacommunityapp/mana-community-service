package com.manacommunity.api.platform.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    @Query("SELECT e FROM OutboxEvent e WHERE e.status IN ('PENDING','RETRY') AND e.nextRetryAt <= :now ORDER BY e.nextRetryAt ASC")
    List<OutboxEvent> findDueEvents(Instant now);
}
