package com.manacommunity.api.adtech.repository;

import com.manacommunity.api.adtech.entity.AdClickEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdClickRepository extends JpaRepository<AdClickEntity, Long> {
}
