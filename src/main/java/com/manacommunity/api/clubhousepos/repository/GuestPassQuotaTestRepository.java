package com.manacommunity.api.clubhousepos.repository;

import com.manacommunity.api.clubhousepos.entity.GuestPassQuotaTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuestPassQuotaTestRepository extends JpaRepository<GuestPassQuotaTest, Long> {}
