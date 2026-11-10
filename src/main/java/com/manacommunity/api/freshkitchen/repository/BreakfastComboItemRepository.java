package com.manacommunity.api.freshkitchen.repository;

import com.manacommunity.api.freshkitchen.model.BreakfastComboItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BreakfastComboItemRepository extends JpaRepository<BreakfastComboItem, Long> {

    List<BreakfastComboItem> findByComboIdOrderBySortOrderAsc(Long comboId);

    List<BreakfastComboItem> findByProductId(String productId);
}
