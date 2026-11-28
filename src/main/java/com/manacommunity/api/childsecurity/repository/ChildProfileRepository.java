package com.manacommunity.api.childsecurity.repository;

import com.manacommunity.api.childsecurity.entity.ChildProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildProfileRepository extends JpaRepository<ChildProfile, Long> {
    List<ChildProfile> findByFlatNumberAndTowerName(String flat, String tower);
    List<ChildProfile> findByBusRouteId(Long routeId);
    List<ChildProfile> findByExitLockEnabledTrue();
}
