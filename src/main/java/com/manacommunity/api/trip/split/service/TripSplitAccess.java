package com.manacommunity.api.trip.split.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.entity.TripBooking;
import com.manacommunity.api.trip.repository.TripBookingRepository;
import com.manacommunity.api.trip.repository.TripRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Who is on a trip and who may do what. Every Trip Split service goes through this. */
@Component
@RequiredArgsConstructor
public class TripSplitAccess {

    private final TripRepository tripRepository;
    private final TripBookingRepository bookingRepository;
    private final AppUserRepository userRepository;

    public Trip requireTrip(String tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
    }

    /** Users with a confirmed booking, plus the organizer. */
    public Set<Long> participantIds(Trip trip) {
        Set<Long> ids = new LinkedHashSet<>();
        if (trip.getOrganizerUserId() != null) {
            ids.add(trip.getOrganizerUserId());
        }
        for (TripBooking booking : bookingRepository.findByTripIdAndStatus(trip.getId(), "CONFIRMED")) {
            if (booking.getUserId() != null) {
                ids.add(booking.getUserId());
            }
        }
        return ids;
    }

    public void requireMember(Set<Long> members, Long userId) {
        if (userId == null || !members.contains(userId)) {
            throw new UnauthorizedActionException("only participants of this trip can use its expense split");
        }
    }

    public boolean isOrganizer(Trip trip, Long userId) {
        return userId != null && userId.equals(trip.getOrganizerUserId());
    }

    public Map<Long, String> names(Collection<Long> userIds) {
        Map<Long, String> names = new LinkedHashMap<>();
        for (AppUser user : userRepository.findAllById(userIds)) {
            names.put(user.getId(), user.getFullName());
        }
        return names;
    }
}
