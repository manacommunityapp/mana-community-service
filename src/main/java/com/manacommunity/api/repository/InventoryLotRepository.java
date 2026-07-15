package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryLotRepository extends JpaRepository<InventoryLot, Long> {

    List<InventoryLot> findByProductIdOrderByNameAsc(Long productId);

    Optional<InventoryLot> findByNameAndProductId(String name, Long productId);

    boolean existsByNameAndProductId(String name, Long productId);
}
