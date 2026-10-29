package com.manacommunity.api.vehicle.repository;

import com.manacommunity.api.vehicle.entity.VehicleRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleRegistrationRepository extends JpaRepository<VehicleRegistration, Long> {
    List<VehicleRegistration> findByOwnerIdAndStatus(Long ownerId, String status);
    boolean existsByPlateNumberAndCommunityId(String plateNumber, Long communityId);
}
