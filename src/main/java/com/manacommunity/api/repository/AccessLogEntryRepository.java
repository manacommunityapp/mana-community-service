package com.manacommunity.api.repository;

import com.manacommunity.api.model.AccessLogEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessLogEntryRepository extends JpaRepository<AccessLogEntry, Long> {

    List<AccessLogEntry> findBySocietyIdOrderByTimestampDesc(Long societyId, Pageable pageable);
}
