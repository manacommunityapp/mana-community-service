package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsWalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VmsWalletTransactionRepository extends JpaRepository<VmsWalletTransaction, Long> {
    Page<VmsWalletTransaction> findByWalletId(Long walletId, Pageable pageable);
    Page<VmsWalletTransaction> findByWalletIdAndType(Long walletId, String type, Pageable pageable);
}
