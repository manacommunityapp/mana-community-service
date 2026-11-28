package com.manacommunity.api.perfbenchmark.repository;

import com.manacommunity.api.perfbenchmark.entity.SyntheticMonitorTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyntheticMonitorTestRepository extends JpaRepository<SyntheticMonitorTest, Long> {}
