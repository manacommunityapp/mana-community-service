package com.manacommunity.api.cfbos.wallet.repository;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface CfbosWalletRepository extends JpaRepository<CfbosWallet, Long> { Optional<CfbosWallet> findByResidentId(Long residentId); }
