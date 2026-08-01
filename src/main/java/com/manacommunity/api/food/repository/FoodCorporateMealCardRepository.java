package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCorporateMealCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodCorporateMealCardRepository extends JpaRepository<FoodCorporateMealCard, Long> {

    List<FoodCorporateMealCard> findByAccountId(Long accountId);

    List<FoodCorporateMealCard> findByUserIdAndCommunityId(Long userId, Long communityId);

    Optional<FoodCorporateMealCard> findByCardNumber(String cardNumber);
}
