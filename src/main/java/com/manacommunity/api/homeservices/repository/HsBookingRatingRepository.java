package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsBookingRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface HsBookingRatingRepository extends JpaRepository<HsBookingRating, Long> {
    Optional<HsBookingRating> findByBookingId(Long bookingId);
    List<HsBookingRating> findByWorkerId(Long workerId);
    @Query("SELECT AVG(r.overall) FROM HsBookingRating r WHERE r.workerId = :workerId")
    Double avgRatingByWorkerId(Long workerId);
}
