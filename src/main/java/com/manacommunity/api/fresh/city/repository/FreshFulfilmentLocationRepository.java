package com.manacommunity.api.fresh.city.repository;

import com.manacommunity.api.fresh.city.model.FreshFulfilmentLocation;
import com.manacommunity.api.fresh.city.model.FulfilmentLocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreshFulfilmentLocationRepository extends JpaRepository<FreshFulfilmentLocation, Long> {
    List<FreshFulfilmentLocation> findByCityId(Long cityId);
    List<FreshFulfilmentLocation> findByCityIdAndLocationType(Long cityId, FulfilmentLocationType locationType);
}
