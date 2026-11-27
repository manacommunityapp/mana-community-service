package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.RecurringBasket;
import com.manacommunity.api.food.entity.RecurringBasket.BasketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface RecurringBasketRepository extends JpaRepository<RecurringBasket, Long> {
    List<RecurringBasket> findByUserIdAndStatusNot(Long userId, BasketStatus status);
    List<RecurringBasket> findByNextOrderDateAndStatus(LocalDate date, BasketStatus status);
}
