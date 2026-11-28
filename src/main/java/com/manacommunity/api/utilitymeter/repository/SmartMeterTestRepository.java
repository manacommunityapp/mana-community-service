package com.manacommunity.api.utilitymeter.repository;

import com.manacommunity.api.utilitymeter.entity.SmartMeterTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmartMeterTestRepository extends JpaRepository<SmartMeterTest, Long> {}
