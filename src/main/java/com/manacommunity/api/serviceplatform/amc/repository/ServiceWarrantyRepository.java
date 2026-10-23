package com.manacommunity.api.serviceplatform.amc.repository;

import com.manacommunity.api.serviceplatform.amc.entity.ServiceWarranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceWarrantyRepository extends JpaRepository<ServiceWarranty, Long> {
    Optional<ServiceWarranty> findByWorkOrderId(Long workOrderId);
}
