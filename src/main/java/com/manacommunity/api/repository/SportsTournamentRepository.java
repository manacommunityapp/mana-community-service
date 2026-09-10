package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsTournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsTournamentRepository extends JpaRepository<SportsTournament, Long> {
    @Query("SELECT t FROM SportsTournament t WHERE t.community.id = :communityId ORDER BY t.createdAt DESC")
    List<SportsTournament> findByEventCommunityIdOrderByCreatedAtDesc(@Param("communityId") Long communityId);

    @Query("SELECT COUNT(t) > 0 FROM SportsTournament t JOIN t.sportsEvents se WHERE se.id = :eventId")
    boolean existsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT DISTINCT t FROM SportsTournament t JOIN t.sportsEvents se WHERE se.id = :eventId")
    List<SportsTournament> findByEventIdList(@Param("eventId") Long eventId);

    default Optional<SportsTournament> findByEventId(Long eventId) {
        List<SportsTournament> list = findByEventIdList(eventId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT DISTINCT t FROM SportsTournament t LEFT JOIN FETCH t.sportsEvents WHERE t.registrationStatus = :status ORDER BY t.createdAt DESC")
    List<SportsTournament> findByRegistrationStatusWithEvents(@Param("status") SportsTournament.EventStatus status);

    @Query("SELECT DISTINCT t FROM SportsTournament t LEFT JOIN FETCH t.sportsEvents se WHERE t.registrationStatus = :status AND t.community.id = :communityId ORDER BY t.createdAt DESC")
    List<SportsTournament> findByRegistrationStatusAndCommunityWithEvents(@Param("status") SportsTournament.EventStatus status, @Param("communityId") Long communityId);

    long countByRegistrationStatusIn(List<SportsTournament.EventStatus> statuses);
    long countByCommunityIdAndRegistrationStatusIn(Long communityId, List<SportsTournament.EventStatus> statuses);

    @Modifying
    @Transactional
    @Query("DELETE FROM SportsTournament t WHERE t.id IN (SELECT t2.id FROM SportsTournament t2 JOIN t2.sportsEvents se WHERE se.id = :eventId)")
    void deleteByEventId(@Param("eventId") Long eventId);
}
