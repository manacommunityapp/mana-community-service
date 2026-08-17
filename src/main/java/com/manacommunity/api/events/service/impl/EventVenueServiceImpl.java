package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventVenue;
import com.manacommunity.api.events.repository.EventVenueRepository;
import com.manacommunity.api.events.service.EventVenueService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventVenueServiceImpl implements EventVenueService {

    private final EventVenueRepository repository;
    private final CommunityRepository communityRepository;

    public EventVenueServiceImpl(EventVenueRepository repository, CommunityRepository communityRepository) {
        this.repository = repository;
        this.communityRepository = communityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventVenue> getVenues(Long communityId, String status) {
        if (communityId != null) {
            if (status != null && !status.isBlank()) {
                return repository.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status);
            }
            return repository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        }
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public EventVenue getVenueById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Venue not found: " + id));
    }

    @Override
    @Transactional
    public EventVenue createVenue(EventVenue venue, Long communityId) {
        Community comm = (communityId != null)
                ? communityRepository.findById(communityId).orElse(null)
                : null;
        venue.setId(null);
        venue.setCommunity(comm);
        if (venue.getStatus() == null || venue.getStatus().isBlank()) {
            venue.setStatus("ACTIVE");
        }
        venue.setCreatedAt(LocalDateTime.now());
        venue.setUpdatedAt(LocalDateTime.now());
        return repository.save(venue);
    }

    @Override
    @Transactional
    public EventVenue updateVenue(Long id, EventVenue venue) {
        EventVenue existing = getVenueById(id);
        if (venue.getName() != null && !venue.getName().isBlank()) {
            existing.setName(venue.getName());
        }
        if (venue.getCode() != null) existing.setCode(venue.getCode());
        if (venue.getAddress() != null) existing.setAddress(venue.getAddress());
        if (venue.getCity() != null) existing.setCity(venue.getCity());
        if (venue.getState() != null) existing.setState(venue.getState());
        if (venue.getPostalCode() != null) existing.setPostalCode(venue.getPostalCode());
        if (venue.getCapacity() != null) existing.setCapacity(venue.getCapacity());
        if (venue.getAmenities() != null) existing.setAmenities(venue.getAmenities());
        if (venue.getGateInfo() != null) existing.setGateInfo(venue.getGateInfo());
        if (venue.getMapCoordinates() != null) existing.setMapCoordinates(venue.getMapCoordinates());
        if (venue.getContactPerson() != null) existing.setContactPerson(venue.getContactPerson());
        if (venue.getContactPhone() != null) existing.setContactPhone(venue.getContactPhone());
        if (venue.getStatus() != null) existing.setStatus(venue.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        return repository.save(existing);
    }

    @Override
    @Transactional
    public void deleteVenue(Long id) {
        repository.deleteById(id);
    }
}