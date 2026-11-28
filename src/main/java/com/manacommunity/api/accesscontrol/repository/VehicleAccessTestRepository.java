package com.manacommunity.api.accesscontrol.repository;

import com.manacommunity.api.accesscontrol.entity.VehicleAccessTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleAccessTestRepository extends JpaRepository<VehicleAccessTest, Long> {}
