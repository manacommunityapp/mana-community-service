package com.manacommunity.api.adtech.repository;

import com.manacommunity.api.adtech.entity.AdImpressionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdImpressionRepository extends JpaRepository<AdImpressionEntity, UUID> {
}
