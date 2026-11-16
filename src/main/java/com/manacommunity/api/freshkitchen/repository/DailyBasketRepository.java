package com.manacommunity.api.freshkitchen.repository;

import com.manacommunity.api.freshkitchen.model.DailyBasket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyBasketRepository extends JpaRepository<DailyBasket, Long> {

    Optional<DailyBasket> findByUserId(Long userId);

    Optional<DailyBasket> findByUserIdAndCommunityId(Long userId, Long communityId);

    List<DailyBasket> findByCommunityIdAndIsActiveTrue(Long communityId);

    List<DailyBasket> findByIsActiveTrue();

    List<DailyBasket> findByIsActiveTrueAndIsPausedFalse();
}
