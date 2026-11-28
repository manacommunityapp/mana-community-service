package com.manacommunity.api.dailysubscription.repository;

import com.manacommunity.api.dailysubscription.entity.WalletRefundTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRefundTestRepository extends JpaRepository<WalletRefundTest, Long> {}
