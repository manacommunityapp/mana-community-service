package com.manacommunity.api.cfbos.wallet.repository;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface CfbosWalletTransactionRepository extends JpaRepository<CfbosWalletTransaction, Long> { List<CfbosWalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId); }
