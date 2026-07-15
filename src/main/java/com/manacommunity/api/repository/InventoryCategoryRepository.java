package com.manacommunity.api.repository;

import com.manacommunity.api.model.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {

    List<InventoryCategory> findByParentIsNullOrderByNameAsc();

    List<InventoryCategory> findByParentIdOrderByNameAsc(Long parentId);
}
