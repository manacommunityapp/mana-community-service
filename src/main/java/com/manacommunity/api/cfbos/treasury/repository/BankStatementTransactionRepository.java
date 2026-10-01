package com.manacommunity.api.cfbos.treasury.repository;

import com.manacommunity.api.cfbos.treasury.entity.BankStatementTransaction;
import com.manacommunity.api.cfbos.treasury.enums.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankStatementTransactionRepository extends JpaRepository<BankStatementTransaction, Long> {
    List<BankStatementTransaction> findByCommunityIdAndStatus(Long communityId, ReconciliationStatus status);
    List<BankStatementTransaction> findByCommunityId(Long communityId);
}
