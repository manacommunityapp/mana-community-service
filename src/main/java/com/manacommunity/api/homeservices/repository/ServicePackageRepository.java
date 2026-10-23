package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.model.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {

    List<ServicePackage> findByResidentIdOrderByCreatedAtDesc(Long residentId);

    List<ServicePackage> findByStaffCommunityIdOrderByCreatedAtDesc(Long communityId);
}
