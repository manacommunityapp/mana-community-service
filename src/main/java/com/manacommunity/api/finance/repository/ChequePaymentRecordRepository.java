package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.ChequePaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChequePaymentRecordRepository extends JpaRepository<ChequePaymentRecord, Long> {

    List<ChequePaymentRecord> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<ChequePaymentRecord> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
}
