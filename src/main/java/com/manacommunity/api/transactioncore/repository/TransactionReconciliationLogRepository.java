package com.manacommunity.api.transactioncore.repository;

import com.manacommunity.api.transactioncore.entity.TransactionReconciliationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionReconciliationLogRepository extends JpaRepository<TransactionReconciliationLog, Long> {
    Optional<TransactionReconciliationLog> findByReconId(String reconId);
    List<TransactionReconciliationLog> findByReconDate(LocalDate reconDate);
    List<TransactionReconciliationLog> findByCommunityIdOrderByExecutedAtDesc(Long communityId);
}
