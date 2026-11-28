package com.manacommunity.api.chaosresilience.repository;

import com.manacommunity.api.chaosresilience.entity.ThirdPartyOutageTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThirdPartyOutageTestRepository extends JpaRepository<ThirdPartyOutageTest, Long> {}
