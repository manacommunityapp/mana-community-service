package com.manacommunity.api.cfbos.charge.repository;

import com.manacommunity.api.cfbos.charge.entity.TierConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TierConfigRepository extends JpaRepository<TierConfig, Long> {
}
