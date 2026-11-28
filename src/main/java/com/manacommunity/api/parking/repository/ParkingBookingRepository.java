package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.ParkingBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingBookingRepository extends JpaRepository<ParkingBooking, Long> {

    List<ParkingBooking> findByCommunityIdAndStatus(Long communityId, String status);

    @Query("SELECT b FROM ParkingBooking b WHERE b.communityId = :communityId AND b.vehicleNumber = :vehicleNumber AND b.status = 'ACTIVE'")
    Optional<ParkingBooking> findActiveByVehicle(@Param("communityId") Long communityId, @Param("vehicleNumber") String vehicleNumber);

    List<ParkingBooking> findByCommunityIdAndWrongParkDetectedTrue(Long communityId);

    @Query("SELECT COUNT(b) FROM ParkingBooking b WHERE b.communityId = :communityId AND b.status = 'ACTIVE'")
    long countActiveBookings(@Param("communityId") Long communityId);
}
