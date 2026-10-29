package com.manacommunity.api.repository;

import com.manacommunity.api.model.VehicleServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleServiceCatalogRepository extends JpaRepository<VehicleServiceCatalog, Long> {

    List<VehicleServiceCatalog> findByIsActiveTrueOrderBySortOrderAsc();
}
