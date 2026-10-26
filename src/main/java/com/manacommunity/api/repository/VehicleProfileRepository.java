package com.manacommunity.api.repository;

import com.manacommunity.api.model.VehicleProfile;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleProfileRepository extends JpaRepository<VehicleProfile, Long> {

    List<VehicleProfile> findByOwnerOrderByIsDefaultDescCreatedAtAsc(AppUser owner);

    Optional<VehicleProfile> findByIdAndOwner(Long id, AppUser owner);

    Optional<VehicleProfile> findByOwnerAndIsDefaultTrue(AppUser owner);

    @Modifying
    @Query("UPDATE VehicleProfile v SET v.isDefault = false WHERE v.owner = :owner")
    void clearDefaultForOwner(@Param("owner") AppUser owner);
}
