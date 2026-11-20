package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsWarranty;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface HsWarrantyRepository extends JpaRepository<HsWarranty, Long> {
    Optional<HsWarranty> findByBookingId(Long bookingId);
}
