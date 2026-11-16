package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyExpenseShareRepository extends JpaRepository<KittyPartyExpenseShare, Long> {

    List<KittyPartyExpenseShare> findByExpenseId(Long expenseId);

    List<KittyPartyExpenseShare> findByUserIdAndSettlementStatus(Long userId, String settlementStatus);

    Optional<KittyPartyExpenseShare> findByExpenseIdAndUserId(Long expenseId, Long userId);
}
