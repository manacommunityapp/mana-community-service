package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsAuctionWatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsAuctionWatchRepository extends JpaRepository<SportsAuctionWatch, Long> {

    List<SportsAuctionWatch> findByUserId(Long userId);

    Optional<SportsAuctionWatch> findByUserIdAndConfigId(Long userId, Long configId);

    boolean existsByUserIdAndConfigId(Long userId, Long configId);

    void deleteByUserIdAndConfigId(Long userId, Long configId);

    long countByConfigId(Long configId);
}
