package com.manacommunity.api.opsresilience.repository;

import com.manacommunity.api.opsresilience.entity.PostmortemGuardrail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostmortemGuardrailRepository extends JpaRepository<PostmortemGuardrail, Long> {
    List<PostmortemGuardrail> findByPostmortemIdOrderByCreatedAtAsc(Long postmortemId);
}
