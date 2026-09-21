package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommuteRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommuteRatingRepository extends JpaRepository<CommuteRating, Long> {

    Optional<CommuteRating> findByRideIdAndRaterId(Long rideId, Long raterId);

    boolean existsByRideIdAndRaterId(Long rideId, Long raterId);

    List<CommuteRating> findByRideId(Long rideId);

    @Query("SELECT COALESCE(AVG(r.score), 0) FROM CommuteRating r WHERE r.rated.id = :userId")
    double getAverageRating(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM CommuteRating r WHERE r.rated.id = :userId")
    long getRatingCount(@Param("userId") Long userId);
}
