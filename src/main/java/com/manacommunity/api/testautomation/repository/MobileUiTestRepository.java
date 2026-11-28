package com.manacommunity.api.testautomation.repository;

import com.manacommunity.api.testautomation.entity.MobileUiTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MobileUiTestRepository extends JpaRepository<MobileUiTest, Long> {}
