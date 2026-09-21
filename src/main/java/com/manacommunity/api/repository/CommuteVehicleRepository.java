package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommuteVehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommuteVehicleRepository extends JpaRepository<CommuteVehicle, Long> {

    List<CommuteVehicle> findByOwnerIdOrderByIsDefaultDesc(Long ownerId);

    Optional<CommuteVehicle> findByOwnerIdAndIsDefaultTrue(Long ownerId);
}
