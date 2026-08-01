package com.cpn.domain.ai.repository;

import com.cpn.domain.ai.model.AiInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiInsightRepository extends JpaRepository<AiInsight, UUID> {
    List<AiInsight> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
