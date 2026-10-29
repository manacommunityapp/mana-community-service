package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.entity.HomeServiceBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeServiceBookingRepository extends JpaRepository<HomeServiceBooking, Long> {
    List<HomeServiceBooking> findByCommunityIdAndResidentUserId(Long communityId, Long residentUserId);
    List<HomeServiceBooking> findByCommunityId(Long communityId);
    List<HomeServiceBooking> findByCommunityIdAndStatus(Long communityId, String status);
}
