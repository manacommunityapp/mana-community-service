package com.manacommunity.api.perfbenchmark.repository;

import com.manacommunity.api.perfbenchmark.entity.LoadTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadTestResultRepository extends JpaRepository<LoadTestResult, Long> {}
