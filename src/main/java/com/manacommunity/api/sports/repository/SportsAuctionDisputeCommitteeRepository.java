package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsAuctionDisputeCommittee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsAuctionDisputeCommitteeRepository extends JpaRepository<SportsAuctionDisputeCommittee, Long> {
    List<SportsAuctionDisputeCommittee> findByConfigId(Long configId);
    void deleteByConfigId(Long configId);
}
