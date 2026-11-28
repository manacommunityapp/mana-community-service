package com.manacommunity.api.accounting.repository;

import com.manacommunity.api.accounting.entity.VendorPanRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPanRegistryRepository extends JpaRepository<VendorPanRegistryEntity, String> {
}
