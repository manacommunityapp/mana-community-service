package com.manacommunity.api.amenity.repository;

import com.manacommunity.api.amenity.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findByCommunityIdAndActiveTrue(Long communityId);

    Optional<Facility> findByCommunityIdAndFacilityCode(Long communityId, String facilityCode);
}
