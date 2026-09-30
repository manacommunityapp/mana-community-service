package com.manacommunity.api.trip.service;

import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.trip.dto.TripRequest;
import com.manacommunity.api.trip.dto.TripResponse;
import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.repository.TripRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<TripResponse> getTrips(Long communityId) {
        return tripRepository.findByCommunityIdOrderByStartDateDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long id, Long communityId) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + id));
        if (!trip.getCommunity().getId().equals(communityId)) {
            throw new UnauthorizedActionException("Trip does not belong to your community");
        }
        return toResponse(trip);
    }

    @Transactional
    public TripResponse createTrip(TripRequest req, AppUser organizer) {
        Trip trip = Trip.builder()
                .community(organizer.getCommunity())
                .organizer(organizer)
                .title(req.title())
                .description(req.description())
                .destination(req.destination())
                .tripType(parseTripType(req.tripType()))
                .startDate(req.startDate())
                .endDate(req.endDate())
                .maxParticipants(req.maxParticipants())
                .estimatedCost(req.estimatedCost())
                .meetingPoint(req.meetingPoint())
                .notes(req.notes())
                .status(Trip.TripStatus.DRAFT)
                .build();

        Trip saved = tripRepository.save(trip);
        auditService.record(AuditAction.TRIP_CREATED, AuditModule.TRIP,
                "Trip", String.valueOf(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public TripResponse updateTrip(Long id, TripRequest req, Long communityId) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + id));
        if (!trip.getCommunity().getId().equals(communityId)) {
            throw new UnauthorizedActionException("Trip does not belong to your community");
        }

        trip.setTitle(req.title());
        trip.setDescription(req.description());
        trip.setDestination(req.destination());
        trip.setTripType(parseTripType(req.tripType()));
        trip.setStartDate(req.startDate());
        trip.setEndDate(req.endDate());
        trip.setMaxParticipants(req.maxParticipants());
        trip.setEstimatedCost(req.estimatedCost());
        trip.setMeetingPoint(req.meetingPoint());
        trip.setNotes(req.notes());

        Trip saved = tripRepository.save(trip);
        auditService.record(AuditAction.TRIP_UPDATED, AuditModule.TRIP,
                "Trip", String.valueOf(id));
        return toResponse(saved);
    }

    @Transactional
    public void cancelTrip(Long id, Long communityId) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + id));
        if (!trip.getCommunity().getId().equals(communityId)) {
            throw new UnauthorizedActionException("Trip does not belong to your community");
        }
        trip.setStatus(Trip.TripStatus.CANCELLED);
        tripRepository.save(trip);
        auditService.record(AuditAction.TRIP_CANCELLED, AuditModule.TRIP,
                "Trip", String.valueOf(id));
    }

    @Transactional
    public void deleteTrip(Long id, Long communityId) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + id));
        if (!trip.getCommunity().getId().equals(communityId)) {
            throw new UnauthorizedActionException("Trip does not belong to your community");
        }
        auditService.record(AuditAction.TRIP_DELETED, AuditModule.TRIP,
                "Trip", String.valueOf(id));
        tripRepository.deleteById(id);
    }

    private Trip.TripType parseTripType(String raw) {
        if (raw == null || raw.isBlank()) return Trip.TripType.DAY_TRIP;
        try { return Trip.TripType.valueOf(raw.toUpperCase()); }
        catch (IllegalArgumentException e) { return Trip.TripType.OTHER; }
    }

    private TripResponse toResponse(Trip t) {
        return TripResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .destination(t.getDestination())
                .tripType(t.getTripType().name())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .maxParticipants(t.getMaxParticipants())
                .currentParticipants(t.getCurrentParticipants())
                .estimatedCost(t.getEstimatedCost())
                .meetingPoint(t.getMeetingPoint())
                .status(t.getStatus().name())
                .organizerName(t.getOrganizer().getFullName())
                .organizerId(t.getOrganizer().getId())
                .notes(t.getNotes())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
