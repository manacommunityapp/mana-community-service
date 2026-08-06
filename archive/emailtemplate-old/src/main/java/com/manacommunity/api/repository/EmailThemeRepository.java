package com.manacommunity.api.repository;

import com.manacommunity.api.model.EmailTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailThemeRepository extends JpaRepository<EmailTheme, Long> {

    List<EmailTheme> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    Optional<EmailTheme> findByCommunityIdAndIsDefaultTrue(Long communityId);

    boolean existsByCommunityIdAndName(Long communityId, String name);
}
