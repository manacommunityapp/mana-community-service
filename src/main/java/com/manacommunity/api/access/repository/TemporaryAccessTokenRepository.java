package com.manacommunity.api.access.repository;

import com.manacommunity.api.access.entity.TemporaryAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemporaryAccessTokenRepository extends JpaRepository<TemporaryAccessToken, Long> {
    List<TemporaryAccessToken> findByCreatedByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<TemporaryAccessToken> findByTokenCode(String tokenCode);
}
