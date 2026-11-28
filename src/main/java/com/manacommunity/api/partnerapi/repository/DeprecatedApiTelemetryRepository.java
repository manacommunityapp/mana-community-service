package com.manacommunity.api.partnerapi.repository;

import com.manacommunity.api.partnerapi.entity.DeprecatedApiTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeprecatedApiTelemetryRepository extends JpaRepository<DeprecatedApiTelemetry, Long> {
    Optional<DeprecatedApiTelemetry> findByEndpointAndControllerSerial(String endpoint, String controllerSerial);
    List<DeprecatedApiTelemetry> findByEndpointOrderByCallCountDesc(String endpoint);
}
