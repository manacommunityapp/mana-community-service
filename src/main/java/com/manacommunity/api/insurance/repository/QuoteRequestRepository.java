package com.manacommunity.api.insurance.repository;

import com.manacommunity.api.insurance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
    Optional<QuoteRequest> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    List<QuoteRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}

