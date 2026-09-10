package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsAuctionSessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SportsAuctionSessionLogRepository extends JpaRepository<SportsAuctionSessionLog, Long> {
}
