package com.manacommunity.api.parking.repository;

import com.manacommunity.api.parking.entity.ParkingVisitorPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingVisitorPassRepository extends JpaRepository<ParkingVisitorPass, Long>, JpaSpecificationExecutor<ParkingVisitorPass> {

    List<ParkingVisitorPass> findByCommunityId(Long communityId);

    List<ParkingVisitorPass> findByUserId(Long userId);

    Optional<ParkingVisitorPass> findByPassCode(String passCode);

    List<ParkingVisitorPass> findByCommunityIdAndStatus(Long communityId, String status);
}
