package com.manacommunity.api.finops.repository;

import com.manacommunity.api.finops.entity.DeliveryRoutingEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryRoutingEventRepository extends JpaRepository<DeliveryRoutingEventEntity, UUID> {
    List<DeliveryRoutingEventEntity> findTop50BySocietyIdOrderByCreatedAtDesc(Long societyId);
    List<DeliveryRoutingEventEntity> findBySocietyId(Long societyId);
}
