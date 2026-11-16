package com.manacommunity.api.fresh.city.repository;

import com.manacommunity.api.fresh.city.model.FreshServiceZonePostalCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreshServiceZonePostalCodeRepository extends JpaRepository<FreshServiceZonePostalCode, Long> {
    List<FreshServiceZonePostalCode> findByPostalCode(String postalCode);
    Optional<FreshServiceZonePostalCode> findByZoneIdAndPostalCode(Long zoneId, String postalCode);
}
