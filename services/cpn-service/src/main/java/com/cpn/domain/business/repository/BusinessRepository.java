package com.cpn.domain.business.repository;

import com.cpn.domain.business.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {
    List<Business> findByCategory(String category);
    List<Business> findByOwnerId(UUID ownerId);
}
