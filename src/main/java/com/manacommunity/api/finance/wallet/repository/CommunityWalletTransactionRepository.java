package com.manacommunity.api.finance.wallet.repository;

import com.manacommunity.api.finance.wallet.entity.CommunityWalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommunityWalletTransactionRepository extends JpaRepository<CommunityWalletTransaction, Long> {
    Page<CommunityWalletTransaction> findByWalletIdOrderByInitiatedAtDesc(Long walletId, Pageable pageable);
    Page<CommunityWalletTransaction> findByCommunityIdOrderByInitiatedAtDesc(Long communityId, Pageable pageable);
    Page<CommunityWalletTransaction> findByCommunityIdAndTxnTypeOrderByInitiatedAtDesc(Long communityId, String txnType, Pageable pageable);
    Optional<CommunityWalletTransaction> findByExternalTxnRef(String externalTxnRef);
}
