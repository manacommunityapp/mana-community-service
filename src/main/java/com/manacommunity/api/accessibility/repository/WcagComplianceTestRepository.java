package com.manacommunity.api.accessibility.repository;

import com.manacommunity.api.accessibility.entity.WcagComplianceTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WcagComplianceTestRepository extends JpaRepository<WcagComplianceTest, Long> {}
