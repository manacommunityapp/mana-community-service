package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsAuctionDisputeCommittee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SportsAuctionDisputeCommitteeRepository extends JpaRepository<SportsAuctionDisputeCommittee, Long> {
}
