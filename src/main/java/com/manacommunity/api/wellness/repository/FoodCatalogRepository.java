package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.FoodCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodCatalogRepository extends JpaRepository<FoodCatalog, Long> {
    @Query("SELECT f FROM FoodCatalog f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(f.nameLocal) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<FoodCatalog> searchByName(String query);

    List<FoodCatalog> findByCategory(String category);
    List<FoodCatalog> findByCuisine(String cuisine);
}
