package com.manacommunity.api.adtech.repository;

import com.manacommunity.api.adtech.entity.AdCreativeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdCreativeRepository extends JpaRepository<AdCreativeEntity, Long> {

    List<AdCreativeEntity> findByCampaignIdAndDeletedFalse(UUID campaignId);
}
