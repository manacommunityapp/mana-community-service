package com.manacommunity.api.mobility.repository;

import com.manacommunity.api.mobility.model.MobilityVehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MobilityVehicleRepository extends JpaRepository<MobilityVehicle, Long> {

    List<MobilityVehicle> findByDriverIdAndActiveTrue(Long driverId);
}
