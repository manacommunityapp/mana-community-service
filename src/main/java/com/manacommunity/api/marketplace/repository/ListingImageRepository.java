package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {
}
