package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsWorkerPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HsWorkerPayoutRepository extends JpaRepository<HsWorkerPayout, Long> {
    List<HsWorkerPayout> findByWorkerIdOrderByCreatedAtDesc(Long workerId);
    List<HsWorkerPayout> findByCommunityIdAndPaidFalse(Long communityId);
}
