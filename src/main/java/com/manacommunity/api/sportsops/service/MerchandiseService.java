package com.manacommunity.api.sportsops.service;

import com.manacommunity.api.sportsops.entity.MerchandiseOrder;
import com.manacommunity.api.sportsops.entity.TournamentMerchandise;
import com.manacommunity.api.sportsops.repository.MerchandiseOrderRepository;
import com.manacommunity.api.sportsops.repository.TournamentMerchandiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchandiseService {
    private final TournamentMerchandiseRepository itemRepo;
    private final MerchandiseOrderRepository orderRepo;

    public List<TournamentMerchandise> getItems(Long tournamentId) {
        return itemRepo.findByTournamentIdAndIsAvailableTrue(tournamentId);
    }

    @Transactional
    public TournamentMerchandise addItem(TournamentMerchandise item) {
        return itemRepo.save(item);
    }

    @Transactional
    public MerchandiseOrder placeOrder(Long merchandiseId, Long userId, Long communityId, int qty) {
        TournamentMerchandise item = itemRepo.findById(merchandiseId)
            .orElseThrow(() -> new IllegalArgumentException("Merchandise not found"));
        if (!Boolean.TRUE.equals(item.getIsAvailable())) throw new IllegalStateException("Item not available");
        if (item.getStockQty() < qty) throw new IllegalStateException("Insufficient stock");
        item.setStockQty(item.getStockQty() - qty);
        if (item.getStockQty() == 0) item.setIsAvailable(false);
        itemRepo.save(item);

        BigDecimal total = item.getPrice().multiply(BigDecimal.valueOf(qty));
        MerchandiseOrder order = MerchandiseOrder.builder()
            .communityId(communityId)
            .merchandiseId(merchandiseId)
            .userId(userId)
            .quantity(qty)
            .unitPrice(item.getPrice())
            .totalAmount(total)
            .build();
        return orderRepo.save(order);
    }

    public List<MerchandiseOrder> getOrdersForUser(Long userId) {
        return orderRepo.findByUserId(userId);
    }
}
