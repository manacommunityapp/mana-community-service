package com.manacommunity.api.datamigration.repository;

import com.manacommunity.api.datamigration.entity.AppUpgradeTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUpgradeTestRepository extends JpaRepository<AppUpgradeTest, Long> {}
