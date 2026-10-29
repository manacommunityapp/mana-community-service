package com.manacommunity.api.repository;

import com.manacommunity.api.model.VehicleServiceAddon;
import com.manacommunity.api.model.VehicleServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleServiceAddonRepository extends JpaRepository<VehicleServiceAddon, Long> {

    List<VehicleServiceAddon> findByServiceAndIsActiveTrueOrderByPriceAsc(VehicleServiceCatalog service);
}
