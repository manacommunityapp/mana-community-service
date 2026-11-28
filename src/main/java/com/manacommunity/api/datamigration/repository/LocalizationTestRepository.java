package com.manacommunity.api.datamigration.repository;

import com.manacommunity.api.datamigration.entity.LocalizationTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalizationTestRepository extends JpaRepository<LocalizationTest, Long> {}
