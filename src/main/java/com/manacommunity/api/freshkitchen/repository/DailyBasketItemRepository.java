package com.manacommunity.api.freshkitchen.repository;

import com.manacommunity.api.freshkitchen.model.DailyBasketItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyBasketItemRepository extends JpaRepository<DailyBasketItem, Long> {

    List<DailyBasketItem> findByBasketId(Long basketId);

    List<DailyBasketItem> findByBasketIdAndIsActiveTrue(Long basketId);

    Optional<DailyBasketItem> findByIdAndBasketId(Long id, Long basketId);

    List<DailyBasketItem> findByProductId(String productId);
}
