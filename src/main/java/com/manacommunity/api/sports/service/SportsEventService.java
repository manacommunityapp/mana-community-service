package com.manacommunity.api.sports.service;
import com.manacommunity.api.sports.controller.SportsController;


import com.manacommunity.api.dto.RegistrationRequest;
import com.manacommunity.api.dto.TeamRegistrationRequest;
import com.manacommunity.api.sports.dto.SportsEventRequest;
import com.manacommunity.api.sports.model.SportsEventRegistration;
import com.manacommunity.api.sports.model.SportsEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * BUG FIX: SportsEventService was a stub. SportsController calls:
 * - createEvent, updateStatus, getOpenEvents, getMyEvents, registerUser, withdraw
 * All must be declared here so the controller compiles.
 */
public interface SportsEventService {
    SportsEvent createEvent(SportsEventRequest req, Long adminUserId);
    SportsEvent updateStatus(Long eventId, String status);
    List<SportsEvent> getOpenEvents(Long communityId);
    List<SportsEvent> getMyEvents(Long userId);
    List<SportsEvent> getAllOpenEvents();
    List<SportsEvent> getClosedEvents();
    List<SportsEvent> getClosedEvents(Long communityId);
    List<SportsEvent> getCompletedEvents();
    List<SportsEvent> getCompletedEvents(Long communityId);
    SportsEventRegistration registerUser(RegistrationRequest req, Long userId);
    SportsEventRegistration registerOthers(RegistrationRequest req, Long requesterUserId);
    List<SportsEventRegistration> registerTeam(TeamRegistrationRequest req, Long captainUserId);
    List<SportsEvent> getAllEvents();
    Page<SportsEvent> getAllEvents(Pageable pageable);
    List<SportsEvent> getAllEventsIncludingInactive();
    List<SportsEvent> getCommunityEvents(Long communityId);
    Page<SportsEvent> getCommunityEvents(Long communityId, Pageable pageable);
    List<SportsEvent> getCommunityEventsIncludingInactive(Long communityId);
    SportsEvent getEventById(Long id);
    SportsEvent getEventByUuid(java.util.UUID uuid);
    SportsEvent saveEvent(SportsEvent event);
    void deleteEvent(Long eventId);
    SportsEvent updateEvent(Long eventId, SportsEventRequest req);
    void withdraw(Long registrationId, Long userId, boolean admin, String reason);
    default void withdraw(Long registrationId, Long userId) { withdraw(registrationId, userId, false, null); }
    List<SportsEventRegistration> getEventRegistrations(Long eventId);
    List<SportsEventRegistration> getEventRegistrations(Long eventId, boolean latestOnly);
    List<SportsEventRegistration> getTournamentAllRegistrations(Long tournamentId);
    List<SportsEventRegistration> getRegistrationsForEventIds(List<Long> eventIds);
    List<SportsEventRegistration> getRegistrationHistory(Long eventId, Long registrationId);
    List<SportsEventRegistration> getUserRegistrations(Long userId);
    SportsEventRegistration confirmRegistration(Long registrationId);
    SportsEventRegistration rejectRegistration(Long registrationId, String reason);
    SportsEventRegistration nominateCaptain(Long registrationId, boolean nominate, String teamName);
    SportsEventRegistration nominateCaptain(Long registrationId, Long requestingUserId, boolean isAdmin, boolean nominate, String teamName);
    SportsEventRegistration confirmCaptain(Long registrationId, boolean confirm);
    SportsEventRegistration respondToPartnerInvitation(Long registrationId, Long partnerUserId, boolean accept, String declineReason);
    List<SportsEventRegistration> getPartnerInvitations(Long partnerUserId, SportsEventRegistration.PartnerConfirmationStatus status);
    default SportsEventRegistration nominatePartner(Long registrationId, Long requestingUserId, Long partnerUserId, Long partnerFamilyMemberId) {
        return nominatePartner(registrationId, requestingUserId, partnerUserId, partnerFamilyMemberId, false, null);
    }
    SportsEventRegistration nominatePartner(Long registrationId, Long requestingUserId, Long partnerUserId, Long partnerFamilyMemberId, boolean isAdmin, Boolean autoConfirm);
    
    java.util.List<java.util.Map<String, Object>> getEventMap(Long communityId);
    long getConfirmedRegistrationCount(Long eventId);
    SportsEvent setCaptainNominationOpen(Long eventId, boolean open);
    SportsEvent updateDisputeCommittee(Long eventId, java.util.List<Long> userIds);
    SportsEventRegistration setRegistrationSeed(Long registrationId, Integer seed);
    List<SportsEvent> batchUpdateStatus(List<Long> eventIds, String status);
    void batchDeleteEvents(List<Long> eventIds);
}
