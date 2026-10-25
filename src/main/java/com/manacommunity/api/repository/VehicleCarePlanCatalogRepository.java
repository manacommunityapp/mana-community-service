package com.manacommunity.api.repository;

import com.manacommunity.api.model.VehicleCarePlanCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleCarePlanCatalogRepository extends JpaRepository<VehicleCarePlanCatalog, Long> {

    List<VehicleCarePlanCatalog> findByIsActiveTrueOrderBySortOrderAsc();
}
