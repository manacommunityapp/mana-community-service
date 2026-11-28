package com.manacommunity.api.dataprivacy.repository;

import com.manacommunity.api.dataprivacy.entity.CctvForensicsTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CctvForensicsTestRepository extends JpaRepository<CctvForensicsTest, Long> {}
