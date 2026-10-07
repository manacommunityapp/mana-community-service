package com.manacommunity.api.transactioncore.repository;

import com.manacommunity.api.transactioncore.entity.TransactionEscrow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionEscrowRepository extends JpaRepository<TransactionEscrow, Long> {
    Optional<TransactionEscrow> findByEscrowId(String escrowId);
    Optional<TransactionEscrow> findByIntentId(String intentId);
    List<TransactionEscrow> findByStatus(String status);
    List<TransactionEscrow> findByPayeeIdAndPayeeType(Long payeeId, String payeeType);
}
