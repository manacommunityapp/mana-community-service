package com.manacommunity.api.repository;

import com.manacommunity.api.model.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {

    List<LedgerAccount> findByCommunityIdAndActiveTrue(Long communityId);

    Optional<LedgerAccount> findByCommunityIdAndAccountCode(Long communityId, String accountCode);

    List<LedgerAccount> findByCommunityIdAndAccountType(Long communityId, String accountType);

    List<LedgerAccount> findByCommunityIdAndIsSystemAccountTrue(Long communityId);
}
