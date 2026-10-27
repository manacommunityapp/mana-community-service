package com.manacommunity.api.repository;

import com.manacommunity.api.model.JewelleryAppointment;
import com.manacommunity.api.model.JewelleryVendorProfile;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JewelleryAppointmentRepository extends JpaRepository<JewelleryAppointment, Long> {

    List<JewelleryAppointment> findByUserOrderByAppointmentDateDescCreatedAtDesc(AppUser user);

    List<JewelleryAppointment> findByVendorAndStatusOrderByAppointmentDateAsc(
            JewelleryVendorProfile vendor, String status);

    Optional<JewelleryAppointment> findByIdAndUser(Long id, AppUser user);

    /** Merchant dashboard: all appointments for this vendor, newest first. */
    List<JewelleryAppointment> findByVendorOrderByAppointmentDateDescCreatedAtDesc(JewelleryVendorProfile vendor);

    /** Security check: only the owning vendor may update appointment status. */
    Optional<JewelleryAppointment> findByIdAndVendor(Long id, JewelleryVendorProfile vendor);

    /** Query active appointments on a given date to compute remaining available slots. */
    List<JewelleryAppointment> findByVendorAndAppointmentDateAndStatusIn(
            JewelleryVendorProfile vendor, java.time.LocalDate appointmentDate, List<String> statuses);
}
