package com.manacommunity.api.federationrisk.repository;

import com.manacommunity.api.federationrisk.entity.CityRiskSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CityRiskSignalRepository extends JpaRepository<CityRiskSignal, Long> {
    Optional<CityRiskSignal> findByCityCodeAndSubjectTokenHashAndSocietyBlindHash(
            String cityCode, String subjectTokenHash, String societyBlindHash);
    List<CityRiskSignal> findByCityCodeAndSubjectTokenHash(String cityCode, String subjectTokenHash);
}
