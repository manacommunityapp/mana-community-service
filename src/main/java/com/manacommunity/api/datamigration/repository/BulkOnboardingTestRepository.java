package com.manacommunity.api.datamigration.repository;

import com.manacommunity.api.datamigration.entity.BulkOnboardingTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkOnboardingTestRepository extends JpaRepository<BulkOnboardingTest, Long> {}
