package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.RecurringBasketItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecurringBasketItemRepository extends JpaRepository<RecurringBasketItem, Long> {
    List<RecurringBasketItem> findByBasketId(Long basketId);
    void deleteByBasketId(Long basketId);
}
