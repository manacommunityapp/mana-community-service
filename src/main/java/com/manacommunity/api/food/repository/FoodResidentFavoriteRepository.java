package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.FoodResidentFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodResidentFavoriteRepository extends JpaRepository<FoodResidentFavorite, Long> {

    List<FoodResidentFavorite> findByProfileIdAndFavoriteType(Long profileId, FoodResidentFavorite.FavoriteType favoriteType);

    List<FoodResidentFavorite> findByProfileId(Long profileId);
}
