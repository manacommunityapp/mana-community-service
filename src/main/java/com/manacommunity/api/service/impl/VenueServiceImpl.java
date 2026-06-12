package com.manacommunity.api.service.impl;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Venue;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.VenueRepository;
import com.manacommunity.api.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepo;
    private final CommunityRepository communityRepo;

    @Override
    public List<Venue> getVenuesByCommunityId(Long communityId) {
        return venueRepo.findByCommunityIdOrCommunityIdIsNull(communityId);
    }

    @Override
    public List<Venue> getAllVenues() {
        return venueRepo.findAll();
    }

    @Override
    public Venue getVenueById(Long id) {
        return venueRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));
    }

    @Override
    @Transactional
    public Venue createVenue(Long communityId, Venue venue) {
        if (communityId != null && communityId > 0) {
            Community community = communityRepo.findById(communityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));
            venue.setCommunity(community);
        }
        if (venue.getCourts() != null) {
            venue.getCourts().forEach(court -> court.setVenue(venue));
        }
        return venueRepo.save(venue);
    }

    @Override
    @Transactional
    public Venue updateVenue(Long id, Venue updatedVenue) {
        Venue existing = venueRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));
        existing.setName(updatedVenue.getName());
        existing.setAddress(updatedVenue.getAddress());
        existing.setCity(updatedVenue.getCity());
        existing.setArea(updatedVenue.getArea());
        existing.setPinCode(updatedVenue.getPinCode());
        existing.setMapLink(updatedVenue.getMapLink());
        existing.setCapacity(updatedVenue.getCapacity());
        existing.setVenueType(updatedVenue.getVenueType());
        existing.setVenueCategory(updatedVenue.getVenueCategory());
        existing.setOpeningTime(updatedVenue.getOpeningTime());
        existing.setClosingTime(updatedVenue.getClosingTime());
        existing.setContactName(updatedVenue.getContactName());
        existing.setContactNumber(updatedVenue.getContactNumber());
        existing.setContactEmail(updatedVenue.getContactEmail());

        // Update courts cleanly
        existing.getCourts().clear();
        if (updatedVenue.getCourts() != null) {
            updatedVenue.getCourts().forEach(court -> {
                court.setVenue(existing);
                existing.getCourts().add(court);
            });
        }

        return venueRepo.save(existing);
    }

    @Override
    @Transactional
    public void deleteVenue(Long id) {
        if (!venueRepo.existsById(id)) {
            throw new ResourceNotFoundException("Venue", id);
        }
        venueRepo.deleteById(id);
    }
}
