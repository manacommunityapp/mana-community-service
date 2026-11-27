package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.VehicleSticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleStickerRepository extends JpaRepository<VehicleSticker, Long> {

    @Query("SELECT s FROM VehicleSticker s WHERE s.communityId = :communityId AND s.vehicleNumber = :vehicleNumber AND s.active = true")
    Optional<VehicleSticker> findByVehicle(@Param("communityId") Long communityId, @Param("vehicleNumber") String vehicleNumber);

    Optional<VehicleSticker> findByStickerCodeAndActiveTrue(String stickerCode);
}
