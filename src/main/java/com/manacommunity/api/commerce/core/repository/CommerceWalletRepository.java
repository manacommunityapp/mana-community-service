package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommerceWalletRepository extends JpaRepository<CommerceWallet, Long> {
    Optional<CommerceWallet> findByUserIdAndCommunityId(Long userId, Long communityId);

    /**
     * Acquires a database-level PESSIMISTIC_WRITE lock on the wallet row.
     * Call this inside a @Transactional method when debiting or crediting
     * the wallet to prevent concurrent balance updates (double-spend).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM CommerceWallet w WHERE w.user.id = :userId AND w.community.id = :communityId")
    Optional<CommerceWallet> findByUserIdAndCommunityIdWithLock(Long userId, Long communityId);
}
