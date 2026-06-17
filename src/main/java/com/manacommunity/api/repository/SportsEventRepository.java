package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsEvent;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SportsEventRepository extends JpaRepository<SportsEvent, Long> {

    /** Look up an event by its public, non-sequential UUID (used in shareable registration links). */
    Optional<SportsEvent> findByUuid(UUID uuid);

    /**
     * BUG FIX: String literals like 'REGISTRATION_OPEN' are INVALID in JPQL
     * for enum-typed fields — JPQL compares against enum names, not string literals.
     * Use com.manacommunity.api.model.SportsEvent$EventStatus enum constants
     * or switch to a Spring Data derived query / @Query with proper enum params.
     *
     * Fixed by using Spring Data method derivation which handles enum properly.
     */
    List<SportsEvent> findByCommunityIdAndTournamentRegistrationStatusInOrderByEventDateStartAsc(
            Long communityId, List<com.manacommunity.api.model.Tournament.EventStatus> registrationStatuses);

    /**
     * BUG FIX: `JOIN EventRegistration r ON r.event.id = e.id` is invalid JPQL.
     * JPQL uses entity relationships, not table joins. Correct form:
     * JOIN e.registrations r (requires a @OneToMany on SportsEvent), or
     * use a subquery/IN clause approach.
     *
     * Fixed using a subquery referencing SportsEventRegistration entity.
     */
    @Query("""
        SELECT DISTINCT e FROM SportsEvent e
        WHERE e.id IN (
            SELECT r.event.id FROM SportsEventRegistration r
            WHERE r.user.id = :userId
        )
        AND e.tournament.registrationStatus <> com.manacommunity.api.model.Tournament$EventStatus.CANCELLED
        ORDER BY e.eventDateStart ASC
    """)
    List<SportsEvent> findEventsForUser(@Param("userId") Long userId);

    /** Find all events by registrationStatus, ordered by start date */
    List<SportsEvent> findByTournamentRegistrationStatusOrderByEventDateStartAsc(com.manacommunity.api.model.Tournament.EventStatus registrationStatus);

    /** Find all events for a specific community */
    List<SportsEvent> findByCommunityIdOrderByEventDateStartDesc(Long communityId);

    List<SportsEvent> findByActiveTrue();

    List<SportsEvent> findByCommunityIdAndActiveTrueOrderByEventDateStartDesc(Long communityId);
}
