package com.manacommunity.api.observability.repository;

import com.manacommunity.api.observability.entity.AlertEscalationTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertEscalationTestRepository extends JpaRepository<AlertEscalationTest, Long> {}
