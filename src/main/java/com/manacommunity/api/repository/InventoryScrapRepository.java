package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryScrapRepository extends JpaRepository<InventoryScrap, Long> {

    List<InventoryScrap> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryScrap> findByStateOrderByCreatedAtDesc(InventoryScrap.ScrapState state);

    List<InventoryScrap> findAllByOrderByCreatedAtDesc();
}
