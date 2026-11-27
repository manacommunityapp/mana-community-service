package com.manacommunity.platform.ops;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformDeployRepository extends JpaRepository<PlatformDeployEvent, Long> {

    List<PlatformDeployEvent> findTop20ByOrderByStartedAtDesc();

    List<PlatformDeployEvent> findByServiceNameOrderByStartedAtDesc(String serviceName);
}
