package com.manacommunity.api.trip.split.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.PrefRequest;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.PrefView;
import com.manacommunity.api.trip.split.entity.MyMoneyMode;
import com.manacommunity.api.trip.split.entity.TripMyMoneyPref;
import com.manacommunity.api.trip.split.repository.TripMyMoneyPrefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A user's own opt-in for reflecting a trip in My Money. Users can only ever read or change their own. */
@Service
@RequiredArgsConstructor
public class TripMyMoneyPrefService {

    private final TripSplitAccess access;
    private final TripMyMoneyPrefRepository prefRepository;
    private final TripMyMoneySync sync;

    @Transactional(readOnly = true)
    public PrefView get(String tripId, Long actorId) {
        Trip trip = access.requireTrip(tripId);
        access.requireMember(access.participantIds(trip), actorId);
        return new PrefView(sync.modeOf(tripId, actorId).name());
    }

    @Transactional
    public PrefView set(String tripId, PrefRequest req, Long actorId) {
        if (req == null || req.mode() == null) {
            throw new InvalidInputException("Mode is required: OFF, SHARE or SETTLEMENTS");
        }
        Trip trip = access.requireTrip(tripId);
        access.requireMember(access.participantIds(trip), actorId);

        MyMoneyMode mode;
        try {
            mode = MyMoneyMode.valueOf(req.mode().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidInputException("Unknown mode: " + req.mode() + ". Use OFF, SHARE or SETTLEMENTS");
        }

        TripMyMoneyPref pref = prefRepository.findByTripIdAndUserId(tripId, actorId)
                .orElseGet(() -> TripMyMoneyPref.builder().tripId(tripId).userId(actorId).build());
        pref.setMode(mode);
        prefRepository.save(pref);

        sync.applyPreference(trip, actorId, mode);
        return new PrefView(mode.name());
    }
}
