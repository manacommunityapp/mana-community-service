package com.manacommunity.api.chaosresilience.repository;

import com.manacommunity.api.chaosresilience.entity.BackwardCompatibilityTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackwardCompatibilityTestRepository extends JpaRepository<BackwardCompatibilityTest, Long> {}
