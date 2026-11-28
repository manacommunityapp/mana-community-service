package com.manacommunity.api.observability.repository;

import com.manacommunity.api.observability.entity.ReleaseTelemetryTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseTelemetryTestRepository extends JpaRepository<ReleaseTelemetryTest, Long> {}
