package com.manacommunity.api.recovery.repository;

import com.manacommunity.api.recovery.entity.GstBackfillJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GstBackfillJobRepository extends JpaRepository<GstBackfillJobEntity, UUID> {

    List<GstBackfillJobEntity> findAllByOrderByCreatedAtDesc();
}
