package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommerceWalletRepository extends JpaRepository<CommerceWallet, Long> {
    Optional<CommerceWallet> findByUserIdAndCommunityId(Long userId, Long communityId);
}
