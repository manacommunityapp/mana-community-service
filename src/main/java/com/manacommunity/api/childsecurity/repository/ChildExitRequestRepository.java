package com.manacommunity.api.childsecurity.repository;

import com.manacommunity.api.childsecurity.entity.ChildExitRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildExitRequestRepository extends JpaRepository<ChildExitRequest, Long> {
    List<ChildExitRequest> findByChildIdOrderByRequestedAtDesc(Long childId);

    @Query("SELECT r FROM ChildExitRequest r WHERE r.status = 'PENDING' ORDER BY r.requestedAt")
    List<ChildExitRequest> findPendingRequests();

    @Query("SELECT r.status, COUNT(r) FROM ChildExitRequest r GROUP BY r.status")
    List<Object[]> countByStatus();

    @Query("SELECT r FROM ChildExitRequest r WHERE r.escalationState IN ('AWAITING_PRIMARY', 'PRIMARY_TIMEOUT_ESCALATE_BACKUP', 'IVR_TRIGGERED')")
    List<ChildExitRequest> findActiveEscalations();
}
