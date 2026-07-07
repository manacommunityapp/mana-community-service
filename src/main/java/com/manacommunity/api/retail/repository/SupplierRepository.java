package com.manacommunity.api.retail.repository;

import com.manacommunity.api.retail.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByCommunityIdOrderByNameAsc(Long communityId);
}
