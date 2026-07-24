package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCorporateMealCardTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodCorporateMealCardTransactionRepository extends JpaRepository<FoodCorporateMealCardTransaction, Long> {

    Page<FoodCorporateMealCardTransaction> findByCardId(Long cardId, Pageable pageable);
}
