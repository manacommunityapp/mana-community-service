package com.manacommunity.api.finance.wallet.repository;

import com.manacommunity.api.finance.wallet.entity.CommunityResidentWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommunityResidentWalletRepository extends JpaRepository<CommunityResidentWallet, Long> {
    Optional<CommunityResidentWallet> findByResidentIdAndCommunityId(Long residentId, Long communityId);
}
