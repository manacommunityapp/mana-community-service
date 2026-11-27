package com.manacommunity.api.calendar.repository;

import com.manacommunity.api.calendar.entity.CalendarExportToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CalendarExportTokenRepository extends JpaRepository<CalendarExportToken, Long> {

    Optional<CalendarExportToken> findByUserId(Long userId);

    Optional<CalendarExportToken> findByToken(String token);
}
