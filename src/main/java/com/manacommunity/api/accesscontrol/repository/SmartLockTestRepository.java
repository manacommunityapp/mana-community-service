package com.manacommunity.api.accesscontrol.repository;

import com.manacommunity.api.accesscontrol.entity.SmartLockTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmartLockTestRepository extends JpaRepository<SmartLockTest, Long> {}
