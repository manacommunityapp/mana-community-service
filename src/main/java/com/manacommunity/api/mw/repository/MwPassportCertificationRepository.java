package com.manacommunity.api.mw.repository;

import com.manacommunity.api.mw.entity.MwPassportCertification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MwPassportCertificationRepository extends JpaRepository<MwPassportCertification, Long> {
    List<MwPassportCertification> findByPassportId(Long passportId);
    List<MwPassportCertification> findByPassportIdAndExpiryDateBefore(Long passportId, LocalDate date);
}
