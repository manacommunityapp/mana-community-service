package com.manacommunity.platform.ops;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformAlertRepository extends JpaRepository<PlatformAlert, Long> {

    List<PlatformAlert> findByResolvedAtIsNullOrderByCreatedAtDesc();

    List<PlatformAlert> findByResolvedAtIsNullAndSeverityOrderByCreatedAtDesc(String severity);
}
