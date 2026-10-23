package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceWalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceWalletTransactionRepository extends JpaRepository<CommerceWalletTransaction, Long> {
    List<CommerceWalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
