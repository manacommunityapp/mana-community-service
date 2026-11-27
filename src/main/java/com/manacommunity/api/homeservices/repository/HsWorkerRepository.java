package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HsWorkerRepository extends JpaRepository<HsWorker, Long> {
    List<HsWorker> findByCommunityIdAndIsActiveTrue(Long communityId);
    List<HsWorker> findByVmsVendorIdAndIsActiveTrue(Long vmsVendorId);
}
