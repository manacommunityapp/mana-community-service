package com.manacommunity.api.booking.repository;

import com.manacommunity.api.booking.entity.ResourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceDocumentRepository extends JpaRepository<ResourceDocument, Long> {

    List<ResourceDocument> findByResourceIdOrderByCreatedAtDesc(Long resourceId);
}
