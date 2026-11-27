package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.ParkingBay;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingBayRepository extends JpaRepository<ParkingBay, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ParkingBay b WHERE b.id = :id")
    Optional<ParkingBay> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT b FROM ParkingBay b WHERE b.communityId = :communityId AND b.bayType = 'VISITOR' AND b.status = 'AVAILABLE' AND (b.vehicleClearance = :clearance OR b.vehicleClearance = 'ANY') ORDER BY b.floorDistanceRank ASC")
    List<ParkingBay> findAvailableVisitorBays(@Param("communityId") Long communityId, @Param("clearance") String clearance);

    @Query("SELECT b FROM ParkingBay b WHERE b.communityId = :communityId AND b.bayType = 'VISITOR' AND b.status = 'AVAILABLE' AND b.basement = :basement AND (b.vehicleClearance = :clearance OR b.vehicleClearance = 'ANY') ORDER BY b.floorDistanceRank ASC")
    List<ParkingBay> findAvailableVisitorBaysInBasement(@Param("communityId") Long communityId, @Param("basement") String basement, @Param("clearance") String clearance);

    List<ParkingBay> findByCommunityIdAndBayType(Long communityId, String bayType);

    List<ParkingBay> findByCommunityId(Long communityId);

    @Query("SELECT b FROM ParkingBay b WHERE b.communityId = :communityId AND b.currentVehicleNumber = :vehicleNumber AND b.status = 'OCCUPIED'")
    Optional<ParkingBay> findByCurrentVehicle(@Param("communityId") Long communityId, @Param("vehicleNumber") String vehicleNumber);

    @Query("SELECT COUNT(b) FROM ParkingBay b WHERE b.communityId = :communityId AND b.bayType = 'VISITOR' AND b.status = 'AVAILABLE'")
    long countAvailableVisitorBays(@Param("communityId") Long communityId);

    @Query("SELECT b.basement, COUNT(b) FROM ParkingBay b WHERE b.communityId = :communityId AND b.bayType = 'VISITOR' AND b.status = 'AVAILABLE' GROUP BY b.basement")
    List<Object[]> countAvailableByBasement(@Param("communityId") Long communityId);
}
