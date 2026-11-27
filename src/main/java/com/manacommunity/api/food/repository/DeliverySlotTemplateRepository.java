package com.manacommunity.api.food.repository;

import com.manacommunity.api.food.entity.DeliverySlotTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeliverySlotTemplateRepository extends JpaRepository<DeliverySlotTemplate, Long> {
    List<DeliverySlotTemplate> findByVendorIdAndCommunityIdAndIsActiveTrue(Long vendorId, Long communityId);
    List<DeliverySlotTemplate> findByIsActiveTrue();
}
