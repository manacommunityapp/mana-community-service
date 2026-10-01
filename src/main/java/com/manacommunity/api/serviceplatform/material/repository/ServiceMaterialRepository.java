package com.manacommunity.api.serviceplatform.material.repository;

import com.manacommunity.api.serviceplatform.material.entity.ServiceMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceMaterialRepository extends JpaRepository<ServiceMaterial, Long> {
    List<ServiceMaterial> findByProviderIdAndActiveTrue(Long providerId);
}
