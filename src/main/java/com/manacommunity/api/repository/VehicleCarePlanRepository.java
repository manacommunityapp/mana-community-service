package com.manacommunity.api.repository;

import com.manacommunity.api.model.VehicleCarePlan;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleCarePlanRepository extends JpaRepository<VehicleCarePlan, Long> {

    List<VehicleCarePlan> findByResidentOrderByCreatedAtDesc(AppUser resident);

    Optional<VehicleCarePlan> findByResidentAndStatus(AppUser resident, String status);

    Optional<VehicleCarePlan> findByIdAndResident(Long id, AppUser resident);
}
