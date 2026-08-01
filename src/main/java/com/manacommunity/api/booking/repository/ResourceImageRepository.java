package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.ResourceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceImageRepository extends JpaRepository<ResourceImage, Long> {

    List<ResourceImage> findByResourceIdOrderByDisplayOrderAsc(Long resourceId);

    Optional<ResourceImage> findByResourceIdAndIsPrimaryTrue(Long resourceId);
}
