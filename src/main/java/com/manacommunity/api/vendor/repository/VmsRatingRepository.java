package com.manacommunity.api.vendor.repository;

import com.manacommunity.api.vendor.entity.VmsRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface VmsRatingRepository extends JpaRepository<VmsRating, Long> {
    Page<VmsRating> findByVendorIdAndStatus(Long vendorId, String status, Pageable pageable);
    Optional<VmsRating> findByVendorIdAndUserIdAndBookingId(Long vendorId, Long userId, Long bookingId);

    @Query("SELECT AVG(r.overallRating) FROM VmsRating r WHERE r.vendor.id = :vendorId AND r.status = 'PUBLISHED'")
    BigDecimal findAverageRatingByVendorId(@Param("vendorId") Long vendorId);

    long countByVendorIdAndStatus(Long vendorId, String status);
}
