package com.manacommunity.api.finance.wallet.repository;

import com.manacommunity.api.finance.wallet.entity.CommunityWalletCreditLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommunityWalletCreditLineRepository extends JpaRepository<CommunityWalletCreditLine, Long> {
    List<CommunityWalletCreditLine> findByWalletIdAndStatus(Long walletId, String status);
}
