package com.manacommunity.api.cfbos.approval.repository;

import com.manacommunity.api.cfbos.approval.entity.ApprovalAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {
    List<ApprovalAction> findByRequestIdOrderByActedAtAsc(Long requestId);
}
