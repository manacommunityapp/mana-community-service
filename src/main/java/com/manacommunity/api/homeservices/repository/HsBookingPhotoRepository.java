package com.manacommunity.api.homeservices.repository;
import com.manacommunity.api.homeservices.entity.HsBookingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HsBookingPhotoRepository extends JpaRepository<HsBookingPhoto, Long> {
    List<HsBookingPhoto> findByBookingId(Long bookingId);
    List<HsBookingPhoto> findByBookingIdAndStage(Long bookingId, String stage);
}
