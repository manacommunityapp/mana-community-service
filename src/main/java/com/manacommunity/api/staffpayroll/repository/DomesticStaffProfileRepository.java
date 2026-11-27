package com.manacommunity.api.staffpayroll.repository;

import com.manacommunity.api.staffpayroll.entity.DomesticStaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DomesticStaffProfileRepository extends JpaRepository<DomesticStaffProfile, Long> {
    Optional<DomesticStaffProfile> findByStaffCode(String staffCode);
}
