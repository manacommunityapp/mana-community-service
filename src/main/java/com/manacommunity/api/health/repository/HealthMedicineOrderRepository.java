package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthMedicineOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthMedicineOrderRepository extends JpaRepository<HealthMedicineOrder, Long> {
    List<HealthMedicineOrder> findByTenantIdAndResidentUserIdOrderByCreatedAtDesc(Long tenantId, Long residentUserId);
    Optional<HealthMedicineOrder> findByOrderUuid(UUID orderUuid);
}
