package com.manacommunity.api.communitypolls.repository;

import com.manacommunity.api.communitypolls.entity.AgmVotingTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgmVotingTestRepository extends JpaRepository<AgmVotingTest, Long> {}
