package com.manacommunity.api.commerce.core.repository;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommerceProductRepository extends JpaRepository<CommerceProduct, Long> {
    List<CommerceProduct> findByChannelAndIsActiveTrue(CommerceChannel channel);
    List<CommerceProduct> findByIsActiveTrue();
    Optional<CommerceProduct> findBySku(String sku);
}
