package com.manacommunity.api.iot.edge.repository;

import com.manacommunity.api.iot.edge.entity.EdgeGatewayDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EdgeGatewayDeviceRepository extends JpaRepository<EdgeGatewayDevice, Long> {

    Optional<EdgeGatewayDevice> findBySerialNumber(String serialNumber);

    List<EdgeGatewayDevice> findByCommunityId(Long communityId);

    List<EdgeGatewayDevice> findByCommunityIdAndIsOnline(Long communityId, Boolean isOnline);

    @Query("SELECT d FROM EdgeGatewayDevice d WHERE d.isOnline = true AND d.lastHeartbeatAt < :cutoff")
    List<EdgeGatewayDevice> findStaleOnlineDevices(@Param("cutoff") Instant cutoff);

    @Query("SELECT d.regionTag, COUNT(d) FROM EdgeGatewayDevice d WHERE d.isOnline = false AND d.offlineSince > :since GROUP BY d.regionTag HAVING COUNT(d) >= :threshold")
    List<Object[]> findRegionsWithMassOutage(@Param("since") Instant since, @Param("threshold") long threshold);

    @Query("SELECT d.ispProvider, COUNT(d) FROM EdgeGatewayDevice d WHERE d.isOnline = false AND d.offlineSince > :since GROUP BY d.ispProvider HAVING COUNT(d) >= :threshold")
    List<Object[]> findIspOutages(@Param("since") Instant since, @Param("threshold") long threshold);

    long countByCommunityIdAndIsOnline(Long communityId, Boolean isOnline);
}
