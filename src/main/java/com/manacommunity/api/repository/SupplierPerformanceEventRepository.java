package com.manacommunity.api.repository;

import com.manacommunity.api.model.PerformanceEventType;
import com.manacommunity.api.model.SupplierPerformanceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierPerformanceEventRepository extends JpaRepository<SupplierPerformanceEvent, Long> {

    List<SupplierPerformanceEvent> findBySupplierIdAndCommunityIdOrderByCreatedAtDesc(
            Long supplierId, Long communityId);

    List<SupplierPerformanceEvent> findByCommunityIdAndEventTypeIn(
            Long communityId, List<PerformanceEventType> types);

    long countBySupplierIdAndCommunityIdAndEventType(
            Long supplierId, Long communityId, PerformanceEventType type);
}
