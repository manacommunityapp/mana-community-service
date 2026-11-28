package com.manacommunity.api.chaosresilience.repository;

import com.manacommunity.api.chaosresilience.entity.SoakStabilityTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SoakStabilityTestRepository extends JpaRepository<SoakStabilityTest, Long> {}
