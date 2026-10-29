package com.manacommunity.api.insurance.repository;

import com.manacommunity.api.insurance.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ClaimsAssistanceCaseRepository extends JpaRepository<ClaimsAssistanceCase, Long> {
    List<ClaimsAssistanceCase> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ClaimsAssistanceCase> findByIdAndUserId(Long id, Long userId);
}

