package com.manacommunity.api.repository;

import com.manacommunity.api.model.AuctionBid;
import com.manacommunity.api.model.AuctionBidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
    List<AuctionBid> findByRfqIdAndStatusOrderByBidAmountAsc(Long rfqId, AuctionBidStatus status);
    Optional<AuctionBid> findByRfqIdAndSupplierIdAndStatusNot(Long rfqId, Long supplierId, AuctionBidStatus excluded);
    List<AuctionBid> findByRfqIdAndSupplierId(Long rfqId, Long supplierId);
}
