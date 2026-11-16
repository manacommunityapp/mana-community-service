package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyEventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyEventAttendanceRepository extends JpaRepository<KittyPartyEventAttendance, Long> {

    List<KittyPartyEventAttendance> findByEventId(Long eventId);

    Optional<KittyPartyEventAttendance> findByEventIdAndUserId(Long eventId, Long userId);

    long countByEventIdAndAttendanceStatus(Long eventId, String attendanceStatus);
}
