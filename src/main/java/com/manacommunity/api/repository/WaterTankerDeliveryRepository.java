package com.manacommunity.api.repository;

import com.manacommunity.api.model.WaterTankerDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WaterTankerDeliveryRepository extends JpaRepository<WaterTankerDelivery, Long> {

    List<WaterTankerDelivery> findByCommunityIdOrderByDeliveryDateDesc(Long communityId);

    List<WaterTankerDelivery> findByCommunityIdAndDeliveryDateBetweenOrderByDeliveryDateDesc(
            Long communityId, LocalDateTime start, LocalDateTime end);
}
