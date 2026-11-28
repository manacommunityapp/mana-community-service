package com.manacommunity.api.edgehardware.repository;

import com.manacommunity.api.edgehardware.entity.EdgeGatewayOtaState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EdgeGatewayOtaStateRepository extends JpaRepository<EdgeGatewayOtaState, Long> {
    Optional<EdgeGatewayOtaState> findByControllerId(String controllerId);
}
