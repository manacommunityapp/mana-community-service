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
}
