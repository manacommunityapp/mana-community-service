package com.manacommunity.api.childsecurity.repository;

import com.manacommunity.api.childsecurity.entity.SchoolBusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolBusRouteRepository extends JpaRepository<SchoolBusRoute, Long> {
    Optional<SchoolBusRoute> findByRfidTagId(String rfidTagId);
    List<SchoolBusRoute> findByIsActiveTrue();
    Optional<SchoolBusRoute> findByRouteCode(String routeCode);
}
