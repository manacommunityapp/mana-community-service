package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsEventFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsEventFormatRepository extends JpaRepository<SportsEventFormat, Long> {
    List<SportsEventFormat> findByEventId(Long eventId);
    Optional<SportsEventFormat> findByEventIdAndFormat(Long eventId, SportsEvent.MatchFormat format);
    void deleteByEventId(Long eventId);
}
