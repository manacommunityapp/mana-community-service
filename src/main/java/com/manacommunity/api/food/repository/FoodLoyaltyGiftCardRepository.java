package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodLoyaltyGiftCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodLoyaltyGiftCardRepository extends JpaRepository<FoodLoyaltyGiftCard, Long> {

    Optional<FoodLoyaltyGiftCard> findByCardNumber(String cardNumber);

    List<FoodLoyaltyGiftCard> findByCommunityIdAndPurchasedById(Long communityId, Long purchasedById);

    List<FoodLoyaltyGiftCard> findByCommunityIdAndGiftedToId(Long communityId, Long giftedToId);

    List<FoodLoyaltyGiftCard> findByPurchasedByIdOrGiftedToId(Long purchasedById, Long giftedToId);
}
