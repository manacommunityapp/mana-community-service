package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryPicking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryPickingRepository extends JpaRepository<InventoryPicking, Long> {

    List<InventoryPicking> findByPickingTypeIdOrderByCreatedAtDesc(Long pickingTypeId);

    List<InventoryPicking> findByStateOrderByCreatedAtDesc(InventoryPicking.PickingState state);

    @Query("SELECT p FROM InventoryPicking p WHERE p.pickingType.id = :ptId AND p.state NOT IN ('DONE','CANCEL') ORDER BY p.createdAt DESC")
    List<InventoryPicking> findPendingByPickingType(@Param("ptId") Long pickingTypeId);

    /** Count transfers to-process (not DONE/CANCEL) per picking type. */
    @Query("SELECT COUNT(p) FROM InventoryPicking p WHERE p.pickingType.id = :ptId AND p.state NOT IN ('DONE','CANCEL')")
    long countToProcess(@Param("ptId") Long pickingTypeId);

    /** Count late transfers (scheduled_date < NOW and not done). */
    @Query("SELECT COUNT(p) FROM InventoryPicking p WHERE p.pickingType.id = :ptId AND p.state NOT IN ('DONE','CANCEL') AND p.scheduledDate < CURRENT_TIMESTAMP")
    long countLate(@Param("ptId") Long pickingTypeId);

    /** Count backorders. */
    @Query("SELECT COUNT(p) FROM InventoryPicking p WHERE p.pickingType.id = :ptId AND p.backorder IS NOT NULL AND p.state NOT IN ('DONE','CANCEL')")
    long countBackorders(@Param("ptId") Long pickingTypeId);
}
