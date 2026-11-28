package com.manacommunity.api.accessibility.repository;

import com.manacommunity.api.accessibility.entity.DeviceFragmentationTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceFragmentationTestRepository extends JpaRepository<DeviceFragmentationTest, Long> {}
