package com.manacommunity.api.childsecurity.repository;

import com.manacommunity.api.childsecurity.entity.BusGateEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusGateEventRepository extends JpaRepository<BusGateEvent, Long> {
    List<BusGateEvent> findByBusRouteIdOrderByScannedAtDesc(Long routeId);
}
