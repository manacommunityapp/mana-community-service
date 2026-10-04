package com.manacommunity.api.cfbos.approval.repository;

import com.manacommunity.api.cfbos.approval.entity.ApprovalRequest;
import com.manacommunity.api.cfbos.shared.enums.ApprovalState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    Optional<ApprovalRequest> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<ApprovalRequest> findByStatus(ApprovalState status);
    List<ApprovalRequest> findBySubmittedBy(Long submittedBy);
}
