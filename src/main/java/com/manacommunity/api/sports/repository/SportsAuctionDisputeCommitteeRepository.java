package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsAuctionDisputeCommittee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SportsAuctionDisputeCommitteeRepository extends JpaRepository<SportsAuctionDisputeCommittee, Long> {
}
