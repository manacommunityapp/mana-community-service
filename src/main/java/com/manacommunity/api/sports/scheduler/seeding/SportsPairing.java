package com.manacommunity.api.sports.scheduler.seeding;
import com.manacommunity.api.sports.dto.SportsPlayoffMatchDraftResponse.ParticipantRef;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import com.manacommunity.api.sports.dto.SportsPlayoffMatchDraftResponse;

/**
 * One first-round pairing produced by a {@link SportsSeedingStrategy}.
 *
 * @param home the higher-priority side (top seed in TRADITIONAL; first drawn otherwise)
 * @param away the opponent, or {@code null} when this is a BYE (home auto-advances)
 */
public record SportsPairing(ParticipantRef home, ParticipantRef away) {

    /** A BYE has no opponent — the home player advances automatically. */
    public boolean isBye() {
        return away == null;
    }
}
