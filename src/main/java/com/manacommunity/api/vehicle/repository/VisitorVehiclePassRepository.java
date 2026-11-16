package com.manacommunity.api.vehicle.repository;

import com.manacommunity.api.vehicle.entity.VisitorVehiclePass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VisitorVehiclePassRepository extends JpaRepository<VisitorVehiclePass, Long> {
    List<VisitorVehiclePass> findByCommunityIdAndValidDateAndStatusOrderByCreatedAtDesc(Long communityId, LocalDate validDate, String status);
    List<VisitorVehiclePass> findByResidentUserIdOrderByCreatedAtDesc(Long residentUserId);
}
