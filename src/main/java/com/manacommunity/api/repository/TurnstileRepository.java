package com.manacommunity.api.repository;

import com.manacommunity.api.model.Turnstile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TurnstileRepository extends JpaRepository<Turnstile, Long> {

    List<Turnstile> findBySocietyId(Long societyId);

    Optional<Turnstile> findByTurnstileCode(String turnstileCode);
}
