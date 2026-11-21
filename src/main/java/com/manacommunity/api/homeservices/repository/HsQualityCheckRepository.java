package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsQualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface HsQualityCheckRepository extends JpaRepository<HsQualityCheck, Long> {
    Optional<HsQualityCheck> findByBookingId(Long bookingId);
}
