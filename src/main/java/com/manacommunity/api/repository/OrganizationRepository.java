package com.manacommunity.api.repository;

import com.manacommunity.api.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
