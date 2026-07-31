package com.cpn.domain.startups.repository;

import com.cpn.domain.startups.model.Startup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StartupRepository extends JpaRepository<Startup, UUID> {
    List<Startup> findByFounderId(UUID founderId);
    List<Startup> findByStage(String stage);
}
