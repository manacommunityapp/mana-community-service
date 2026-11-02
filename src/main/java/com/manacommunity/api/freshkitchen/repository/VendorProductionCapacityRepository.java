package com.manacommunity.api.freshkitchen.repository;

import com.manacommunity.api.freshkitchen.model.VendorProductionCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorProductionCapacityRepository extends JpaRepository<VendorProductionCapacity, Long> {

    Optional<VendorProductionCapacity> findByVendorIdAndProductIdAndProductionDate(
            String vendorId, String productId, LocalDate productionDate);

    List<VendorProductionCapacity> findByVendorIdAndProductionDate(String vendorId, LocalDate productionDate);

    List<VendorProductionCapacity> findByProductIdAndProductionDate(String productId, LocalDate productionDate);

    List<VendorProductionCapacity> findByProductionDate(LocalDate productionDate);

    List<VendorProductionCapacity> findByVendorIdOrderByProductionDateDesc(String vendorId);
}
