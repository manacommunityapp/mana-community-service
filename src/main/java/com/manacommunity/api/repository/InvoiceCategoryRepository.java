package com.manacommunity.api.repository;

import com.manacommunity.api.model.InvoiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceCategoryRepository extends JpaRepository<InvoiceCategory, Long> {

    List<InvoiceCategory> findByActiveTrueOrderByNameAsc();

    List<InvoiceCategory> findByCommunityIdAndActiveTrueOrderByNameAsc(Long communityId);

    boolean existsByNameIgnoreCase(String name);
}
