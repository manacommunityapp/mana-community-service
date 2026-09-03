package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventPoojaBookingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventPoojaBookingParticipantRepository extends JpaRepository<EventPoojaBookingParticipant, Long> {

    List<EventPoojaBookingParticipant> findByRegistrationIdOrderByIdAsc(Long registrationId);

    @Modifying
    @Query("DELETE FROM EventPoojaBookingParticipant p WHERE p.registration.id = :registrationId")
    void deleteByRegistrationId(@Param("registrationId") Long registrationId);
}
