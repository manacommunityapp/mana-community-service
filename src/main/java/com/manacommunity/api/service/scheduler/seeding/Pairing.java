package com.manacommunity.api.service.scheduler.seeding;

import com.manacommunity.api.dto.scheduler.PlayoffMatchDraftResponse.ParticipantRef;

/**
 * One first-round pairing produced by a {@link SeedingStrategy}.
 *
 * @param home the higher-priority side (top seed in TRADITIONAL; first drawn otherwise)
 * @param away the opponent, or {@code null} when this is a BYE (home auto-advances)
 */
public record Pairing(ParticipantRef home, ParticipantRef away) {

    /** A BYE has no opponent — the home player advances automatically. */
    public boolean isBye() {
        return away == null;
    }
}
