package com.manacommunity.api.testautomation.repository;

import com.manacommunity.api.testautomation.entity.HardwareSimulatorTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HardwareSimulatorTestRepository extends JpaRepository<HardwareSimulatorTest, Long> {}
