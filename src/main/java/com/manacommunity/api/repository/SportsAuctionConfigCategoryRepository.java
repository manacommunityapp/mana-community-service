package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsAuctionConfigCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SportsAuctionConfigCategoryRepository extends JpaRepository<SportsAuctionConfigCategory, Long> {
}
