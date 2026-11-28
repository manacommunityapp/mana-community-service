package com.manacommunity.api.accessibility.repository;

import com.manacommunity.api.accessibility.entity.OsUpgradeTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OsUpgradeTestRepository extends JpaRepository<OsUpgradeTest, Long> {}
