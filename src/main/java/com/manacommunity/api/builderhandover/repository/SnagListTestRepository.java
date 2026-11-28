package com.manacommunity.api.builderhandover.repository;

import com.manacommunity.api.builderhandover.entity.SnagListTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnagListTestRepository extends JpaRepository<SnagListTest, Long> {}
