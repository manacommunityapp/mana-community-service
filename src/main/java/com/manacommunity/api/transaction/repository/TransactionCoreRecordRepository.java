package com.manacommunity.api.transaction.repository;

import com.manacommunity.api.transaction.entity.TransactionCoreRecord;
import com.manacommunity.api.transaction.enums.TransactionDomain;
import com.manacommunity.api.transaction.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionCoreRecordRepository extends JpaRepository<TransactionCoreRecord, Long> {

    Optional<TransactionCoreRecord> findByTransactionNumber(String transactionNumber);

    Optional<TransactionCoreRecord> findByIdempotencyKey(String idempotencyKey);

    Page<TransactionCoreRecord> findByPayerId(Long payerId, Pageable pageable);

    Page<TransactionCoreRecord> findByPayeeId(Long payeeId, Pageable pageable);

    Page<TransactionCoreRecord> findByDomain(TransactionDomain domain, Pageable pageable);

    Page<TransactionCoreRecord> findByStatus(TransactionStatus status, Pageable pageable);

    List<TransactionCoreRecord> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);
}
