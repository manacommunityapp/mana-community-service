package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryMoveLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryMoveLineRepository extends JpaRepository<InventoryMoveLine, Long> {
    List<InventoryMoveLine> findByPickingIdOrderByIdAsc(Long pickingId);
}
