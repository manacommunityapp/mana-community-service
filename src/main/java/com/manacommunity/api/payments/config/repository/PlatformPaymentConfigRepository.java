package com.manacommunity.api.payments.config.repository;

import com.manacommunity.api.payments.config.entity.PlatformPaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformPaymentConfigRepository extends JpaRepository<PlatformPaymentConfig, Long> {
    Optional<PlatformPaymentConfig> findFirstByOrderByIdAsc();
}
