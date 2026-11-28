package com.manacommunity.api.communitypolls.repository;

import com.manacommunity.api.communitypolls.entity.DirectoryPrivacyTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectoryPrivacyTestRepository extends JpaRepository<DirectoryPrivacyTest, Long> {}
