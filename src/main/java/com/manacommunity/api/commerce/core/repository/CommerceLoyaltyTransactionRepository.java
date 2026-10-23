package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceLoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceLoyaltyTransactionRepository extends JpaRepository<CommerceLoyaltyTransaction, Long> {
    List<CommerceLoyaltyTransaction> findByAccount_IdOrderByCreatedAtDesc(Long accountId);
}
