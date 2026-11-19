package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchInnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SportsMatchInningsRepository extends JpaRepository<SportsMatchInnings, Long> {
    List<SportsMatchInnings> findByMatchResultIdOrderByInningsNumber(Long matchResultId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsMatchInnings i WHERE i.matchResult.id IN " +
           "(SELECT r.id FROM SportsMatchResult r WHERE r.match.config.id = :configId)")
    void deleteByConfigId(@Param("configId") Long configId);
}
