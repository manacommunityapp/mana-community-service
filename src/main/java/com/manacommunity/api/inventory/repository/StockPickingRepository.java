package com.manacommunity.api.inventory.repository;

import com.manacommunity.api.inventory.entity.PickingState;
import com.manacommunity.api.inventory.entity.StockPicking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockPickingRepository extends JpaRepository<StockPicking, Long> {
    Optional<StockPicking> findByName(String name);
    List<StockPicking> findByPickingTypeId(Long pickingTypeId);
    List<StockPicking> findByState(PickingState state);
}
