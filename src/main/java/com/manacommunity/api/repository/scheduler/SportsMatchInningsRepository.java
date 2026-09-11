package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsMatchInnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SportsMatchInningsRepository extends JpaRepository<SportsMatchInnings, Long> {
    List<SportsMatchInnings> findByMatchResultIdOrderByInningsNumber(Long matchResultId);
}
