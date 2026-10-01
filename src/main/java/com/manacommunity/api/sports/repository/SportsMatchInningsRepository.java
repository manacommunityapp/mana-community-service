package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchInnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SportsMatchInningsRepository extends JpaRepository<SportsMatchInnings, Long> {
    List<SportsMatchInnings> findByMatchResultIdOrderByInningsNumber(Long matchResultId);
}
