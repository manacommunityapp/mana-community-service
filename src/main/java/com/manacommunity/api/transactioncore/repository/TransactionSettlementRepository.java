package com.manacommunity.api.transactioncore.repository;

import com.manacommunity.api.transactioncore.entity.TransactionSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionSettlementRepository extends JpaRepository<TransactionSettlement, Long> {
    Optional<TransactionSettlement> findBySettlementId(String settlementId);
    List<TransactionSettlement> findByIntentId(String intentId);
    List<TransactionSettlement> findByBatchId(String batchId);
    List<TransactionSettlement> findByPayeeIdAndPayeeType(Long payeeId, String payeeType);
    List<TransactionSettlement> findByStatus(String status);
}
