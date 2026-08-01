package com.manacommunity.api.email.builder.repository;

import com.manacommunity.api.email.builder.entity.EmailThemeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailThemeConfigRepository extends JpaRepository<EmailThemeConfig, Long> {

    List<EmailThemeConfig> findByCommunityIdOrderByUpdatedAtDesc(Long communityId);

    Optional<EmailThemeConfig> findByIdAndCommunityId(Long id, Long communityId);

    Optional<EmailThemeConfig> findByCommunityIdAndIsDefaultTrue(Long communityId);
}
