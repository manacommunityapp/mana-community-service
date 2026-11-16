package com.manacommunity.api.fresh.recall.repository;

import com.manacommunity.api.fresh.recall.model.FreshProductRecall;
import com.manacommunity.api.fresh.recall.model.RecallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshProductRecallRepository extends JpaRepository<FreshProductRecall, Long> {

    List<FreshProductRecall> findByStatus(RecallStatus status);

    List<FreshProductRecall> findByVendorProductId(Long vendorProductId);

    List<FreshProductRecall> findByBatchId(Long batchId);

    Optional<FreshProductRecall> findByRecallReference(String recallReference);
}
