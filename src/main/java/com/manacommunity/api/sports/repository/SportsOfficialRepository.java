package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsOfficial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsOfficialRepository extends JpaRepository<SportsOfficial, Long> {

    List<SportsOfficial> findByIsAvailableTrue();

    List<SportsOfficial> findBySportIgnoreCaseAndIsAvailableTrue(String sport);
}
