package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HsBookingRepository extends JpaRepository<HsBooking, Long> {
    List<HsBooking> findByCommunityIdAndStatus(Long communityId, String status);
    List<HsBooking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<HsBooking> findByWorkerIdAndStatus(Long workerId, String status);
}
