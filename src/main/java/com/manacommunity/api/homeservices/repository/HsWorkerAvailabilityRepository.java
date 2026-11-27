package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsWorkerAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface HsWorkerAvailabilityRepository extends JpaRepository<HsWorkerAvailability, Long> {
    List<HsWorkerAvailability> findByCommunityIdAndAvailableDateAndIsBookedFalse(Long communityId, LocalDate date);
    List<HsWorkerAvailability> findByWorkerIdAndAvailableDateGreaterThanEqual(Long workerId, LocalDate from);
}
