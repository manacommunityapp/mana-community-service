package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsReworkRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HsReworkRequestRepository extends JpaRepository<HsReworkRequest, Long> {
    List<HsReworkRequest> findByBookingId(Long bookingId);
    List<HsReworkRequest> findByCommunityIdAndStatus(Long communityId, String status);
}
