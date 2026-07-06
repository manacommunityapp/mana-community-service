package com.manacommunity.api.retail.repository;

import com.manacommunity.api.retail.entity.RetailProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetailProductRepository extends JpaRepository<RetailProduct, Long> {

    List<RetailProduct> findByCommunityIdOrderByNameAsc(Long communityId);
}
