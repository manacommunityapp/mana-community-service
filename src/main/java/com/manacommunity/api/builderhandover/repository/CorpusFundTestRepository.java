package com.manacommunity.api.builderhandover.repository;

import com.manacommunity.api.builderhandover.entity.CorpusFundTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CorpusFundTestRepository extends JpaRepository<CorpusFundTest, Long> {}
