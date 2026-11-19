package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.MerchandiseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MerchandiseOrderRepository extends JpaRepository<MerchandiseOrder, Long> {
    List<MerchandiseOrder> findByUserId(Long userId);
    List<MerchandiseOrder> findByMerchandiseId(Long merchandiseId);
}
