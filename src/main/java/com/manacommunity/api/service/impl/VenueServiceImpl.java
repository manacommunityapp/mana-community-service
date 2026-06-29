package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.VenueRequest;
import com.manacommunity.api.dto.VenueResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Court;
import com.manacommunity.api.model.Venue;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.VenueRepository;
import com.manacommunity.api.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepo;
    private final CommunityRepository communityRepo;

    @Override
    public List<VenueResponse> getVenuesByCommunityId(Long communityId) {
        return venueRepo.findByCommunityIdOrCommunityIdIsNull(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<VenueResponse> getAllVenues() {
        return venueRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public Venue getVenueById(Long id) {
        return venueRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));
    }

    @Override
    @Transactional
    public VenueResponse createVenue(Long communityId, VenueRequest request) {
        Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .area(request.getArea())
                .pinCode(request.getPinCode())
                .mapLink(request.getMapLink())
                .capacity(request.getCapacity())
                .venueType(request.getVenueType())
                .venueCategory(request.getVenueCategory())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .contactName(request.getContactName())
                .contactNumber(request.getContactNumber())
                .contactEmail(request.getContactEmail())
                .build();

        if (communityId != null && communityId > 0) {
            Community community = communityRepo.findById(communityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));
            venue.setCommunity(community);
        }
        applyCourts(venue, request.getCourts());
        return toResponse(venueRepo.save(venue));
    }

    @Override
    @Transactional
    public VenueResponse updateVenue(Long id, VenueRequest request) {
        Venue existing = venueRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));
        existing.setName(request.getName());
        existing.setAddress(request.getAddress());
        existing.setCity(request.getCity());
        existing.setArea(request.getArea());
        existing.setPinCode(request.getPinCode());
        existing.setMapLink(request.getMapLink());
        existing.setCapacity(request.getCapacity());
        existing.setVenueType(request.getVenueType());
        existing.setVenueCategory(request.getVenueCategory());
        existing.setOpeningTime(request.getOpeningTime());
        existing.setClosingTime(request.getClosingTime());
        existing.setContactName(request.getContactName());
        existing.setContactNumber(request.getContactNumber());
        existing.setContactEmail(request.getContactEmail());

        existing.getCourts().clear();
        applyCourts(existing, request.getCourts());

        return toResponse(venueRepo.save(existing));
    }

    @Override
    @Transactional
    public void deleteVenue(Long id) {
        if (!venueRepo.existsById(id)) {
            throw new ResourceNotFoundException("Venue", id);
        }
        venueRepo.deleteById(id);
    }

    private void applyCourts(Venue venue, List<VenueRequest.CourtDto> courtDtos) {
        if (courtDtos == null) return;
        for (VenueRequest.CourtDto dto : courtDtos) {
            Court court = Court.builder()
                    .name(dto.getName())
                    .color(dto.getColor())
                    .openingTime(dto.getOpeningTime())
                    .closingTime(dto.getClosingTime())
                    .venue(venue)
                    .build();
            venue.getCourts().add(court);
        }
    }

    private VenueResponse toResponse(Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .area(venue.getArea())
                .pinCode(venue.getPinCode())
                .mapLink(venue.getMapLink())
                .capacity(venue.getCapacity())
                .venueType(venue.getVenueType())
                .venueCategory(venue.getVenueCategory())
                .openingTime(venue.getOpeningTime())
                .closingTime(venue.getClosingTime())
                .contactName(venue.getContactName())
                .contactNumber(venue.getContactNumber())
                .contactEmail(venue.getContactEmail())
                .communityId(venue.getCommunity() != null ? venue.getCommunity().getId() : null)
                .communityName(venue.getCommunity() != null ? venue.getCommunity().getName() : null)
                .courts(venue.getCourts() != null
                        ? venue.getCourts().stream().map(c -> VenueResponse.CourtDto.builder()
                            .id(c.getId())
                            .name(c.getName())
                            .color(c.getColor())
                            .openingTime(c.getOpeningTime())
                            .closingTime(c.getClosingTime())
                            .build()).toList()
                        : List.of())
                .build();
    }
}
