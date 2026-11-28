package com.manacommunity.api.dataprivacy.repository;

import com.manacommunity.api.dataprivacy.entity.DpdpConsentTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DpdpConsentTestRepository extends JpaRepository<DpdpConsentTest, Long> {}
