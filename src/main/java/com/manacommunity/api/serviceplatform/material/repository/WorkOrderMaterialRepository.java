package com.manacommunity.api.serviceplatform.material.repository;

import com.manacommunity.api.serviceplatform.material.entity.WorkOrderMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderMaterialRepository extends JpaRepository<WorkOrderMaterial, Long> {
    List<WorkOrderMaterial> findByWorkOrderId(Long workOrderId);
}
