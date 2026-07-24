package com.manacommunity.api.serviceplatform.repository;

import com.manacommunity.api.serviceplatform.entity.ServiceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findByDomainIdAndActiveTrueOrderByDisplayOrderAsc(Long domainId);

    List<ServiceCategory> findByDomainIdAndParentCategoryIsNullAndActiveTrueOrderByDisplayOrderAsc(Long domainId);

    boolean existsBySlugAndDomainId(String slug, Long domainId);

    @Query("SELECT c FROM ServiceCategory c WHERE c.domain.id = :domainId AND c.active = true " +
           "AND LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ServiceCategory> searchByName(@Param("domainId") Long domainId,
                                       @Param("query") String query,
                                       Pageable pageable);
}
