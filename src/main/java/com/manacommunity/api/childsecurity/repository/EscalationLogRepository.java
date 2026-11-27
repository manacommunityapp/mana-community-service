package com.manacommunity.api.childsecurity.repository;

import com.manacommunity.api.childsecurity.entity.EscalationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscalationLogRepository extends JpaRepository<EscalationLog, Long> {
    List<EscalationLog> findByExitRequestIdOrderByAttemptNumber(Long exitRequestId);
}
