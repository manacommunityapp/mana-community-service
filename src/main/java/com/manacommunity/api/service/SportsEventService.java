package com.manacommunity.api.service;

import com.manacommunity.api.dto.RegistrationRequest;
import com.manacommunity.api.dto.SportsEventRequest;
import com.manacommunity.api.model.SportsEventRegistration;
import com.manacommunity.api.model.SportsEvent;

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
    SportsEventRegistration registerUser(RegistrationRequest req, Long userId);
    List<SportsEvent> getAllEvents();
    Page<SportsEvent> getAllEvents(Pageable pageable);
    List<SportsEvent> getCommunityEvents(Long communityId);
    Page<SportsEvent> getCommunityEvents(Long communityId, Pageable pageable);
    SportsEvent getEventById(Long id);
    SportsEvent getEventByUuid(java.util.UUID uuid);
    SportsEvent saveEvent(SportsEvent event);
    void deleteEvent(Long eventId);
    SportsEvent updateEvent(Long eventId, SportsEventRequest req);
    void withdraw(Long registrationId, Long userId);
    List<SportsEventRegistration> getEventRegistrations(Long eventId);
    List<SportsEventRegistration> getUserRegistrations(Long userId);
    SportsEventRegistration confirmRegistration(Long registrationId);
    SportsEventRegistration rejectRegistration(Long registrationId, String reason);
    SportsEventRegistration nominateCaptain(Long registrationId, boolean nominate, String teamName);
    SportsEventRegistration confirmCaptain(Long registrationId, boolean confirm);
    
    java.util.List<java.util.Map<String, Object>> getEventMap(Long communityId);
    long getConfirmedRegistrationCount(Long eventId);
    
    // Player Category CRUD
    com.manacommunity.api.model.PlayerCategory createCategory(com.manacommunity.api.dto.PlayerCategoryRequest req);
    com.manacommunity.api.model.PlayerCategory updateCategory(Long id, com.manacommunity.api.dto.PlayerCategoryRequest req);
    void deleteCategory(Long id);
}
