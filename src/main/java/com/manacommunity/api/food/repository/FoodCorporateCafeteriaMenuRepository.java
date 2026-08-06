package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodCorporateCafeteriaMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodCorporateCafeteriaMenuRepository extends JpaRepository<FoodCorporateCafeteriaMenu, Long> {

    List<FoodCorporateCafeteriaMenu> findByCafeteriaIdAndDate(Long cafeteriaId, LocalDate date);
}
