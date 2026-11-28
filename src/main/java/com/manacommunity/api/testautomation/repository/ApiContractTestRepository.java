package com.manacommunity.api.testautomation.repository;

import com.manacommunity.api.testautomation.entity.ApiContractTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiContractTestRepository extends JpaRepository<ApiContractTest, Long> {}
