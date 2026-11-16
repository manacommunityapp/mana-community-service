package com.manacommunity.api.repository;

import com.manacommunity.api.model.JewelleryTryOnSession;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JewelleryTryOnSessionRepository extends JpaRepository<JewelleryTryOnSession, Long> {

    List<JewelleryTryOnSession> findByUserOrderByStartedAtDesc(AppUser user, Pageable pageable);

    long countByUserAndMode(AppUser user, String mode);
}
