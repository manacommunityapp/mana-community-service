package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsAuctionSessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SportsAuctionSessionLogRepository extends JpaRepository<SportsAuctionSessionLog, Long> {
}
