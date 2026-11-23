package com.manacommunity.api.devices.repository;

import com.manacommunity.api.devices.entity.SmartDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmartDeviceRepository extends JpaRepository<SmartDevice, Long> {
    List<SmartDevice> findByCommunityId(Long communityId);
    List<SmartDevice> findByCommunityIdAndDeviceType(Long communityId, String deviceType);
}
