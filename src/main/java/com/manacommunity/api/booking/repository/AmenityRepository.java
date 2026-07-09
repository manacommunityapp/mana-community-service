package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    List<Amenity> findByCommunityIdAndActiveTrueOrderByNameAsc(Long communityId);

    List<Amenity> findByCommunityIdOrderByNameAsc(Long communityId);
}
