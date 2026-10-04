package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsAuctionConfigCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SportsAuctionConfigCategoryRepository extends JpaRepository<SportsAuctionConfigCategory, Long> {
}
