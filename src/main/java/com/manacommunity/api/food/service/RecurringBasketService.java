package com.manacommunity.api.food.service;

import com.manacommunity.api.food.entity.RecurringBasket;
import com.manacommunity.api.food.entity.RecurringBasket.BasketFrequency;
import com.manacommunity.api.food.entity.RecurringBasket.BasketStatus;
import com.manacommunity.api.food.entity.RecurringBasketItem;
import com.manacommunity.api.food.repository.RecurringBasketItemRepository;
import com.manacommunity.api.food.repository.RecurringBasketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringBasketService {

    private final RecurringBasketRepository basketRepo;
    private final RecurringBasketItemRepository itemRepo;

    @Transactional
    public RecurringBasket createBasket(RecurringBasket basket, List<RecurringBasketItem> items) {
        basket.setStatus(BasketStatus.ACTIVE);
        basket.setCreatedAt(Instant.now());
        basket.setUpdatedAt(Instant.now());
        RecurringBasket saved = basketRepo.save(basket);
        items.forEach(item -> { item.setBasketId(saved.getId()); itemRepo.save(item); });
        return saved;
    }

    @Transactional
    public RecurringBasket updateBasket(Long basketId, Long userId, RecurringBasket updated, List<RecurringBasketItem> items) {
        RecurringBasket existing = basketRepo.findById(basketId).orElseThrow();
        if (!existing.getUserId().equals(userId)) throw new IllegalStateException("Not owner");
        existing.setName(updated.getName());
        existing.setFrequency(updated.getFrequency());
        existing.setNextOrderDate(updated.getNextOrderDate());
        existing.setUpdatedAt(Instant.now());
        itemRepo.deleteByBasketId(basketId);
        items.forEach(item -> { item.setBasketId(basketId); itemRepo.save(item); });
        return basketRepo.save(existing);
    }

    @Transactional
    public void pauseBasket(Long id) { updateStatus(id, BasketStatus.PAUSED); }

    @Transactional
    public void resumeBasket(Long id) { updateStatus(id, BasketStatus.ACTIVE); }

    @Transactional
    public void cancelBasket(Long id) { updateStatus(id, BasketStatus.CANCELLED); }

    private void updateStatus(Long id, BasketStatus status) {
        basketRepo.findById(id).ifPresent(b -> { b.setStatus(status); b.setUpdatedAt(Instant.now()); basketRepo.save(b); });
    }

    public List<RecurringBasket> getBaskets(Long userId) {
        return basketRepo.findByUserIdAndStatusNot(userId, BasketStatus.CANCELLED);
    }

    public List<RecurringBasketItem> getItems(Long basketId) {
        return itemRepo.findByBasketId(basketId);
    }

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void processRecurringOrders() {
        LocalDate today = LocalDate.now();
        List<RecurringBasket> due = basketRepo.findByNextOrderDateAndStatus(today, BasketStatus.ACTIVE);
        for (RecurringBasket basket : due) {
            log.info("[RecurringBasket] Initiating order for basket={} user={}", basket.getId(), basket.getUserId());
            // Advance next_order_date
            LocalDate next = switch (basket.getFrequency()) {
                case DAILY      -> today.plusDays(1);
                case WEEKLY     -> today.plusWeeks(1);
                case FORTNIGHTLY -> today.plusWeeks(2);
                case MONTHLY    -> today.plusMonths(1);
            };
            basket.setNextOrderDate(next);
            basket.setUpdatedAt(Instant.now());
            basketRepo.save(basket);
        }
        log.info("[RecurringBasket] Processed {} recurring baskets", due.size());
    }
}
