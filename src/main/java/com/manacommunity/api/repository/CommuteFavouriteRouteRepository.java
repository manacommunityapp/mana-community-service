package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommuteFavouriteRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommuteFavouriteRouteRepository extends JpaRepository<CommuteFavouriteRoute, Long> {

    List<CommuteFavouriteRoute> findByUserIdOrderByCreatedAtDesc(Long userId);
}
