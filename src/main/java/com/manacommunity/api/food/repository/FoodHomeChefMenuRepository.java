package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodHomeChefMenu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodHomeChefMenuRepository extends JpaRepository<FoodHomeChefMenu, Long> {

    List<FoodHomeChefMenu> findByChefIdAndActive(Long chefId, Boolean active);

    Page<FoodHomeChefMenu> findByChefId(Long chefId, Pageable pageable);
}
