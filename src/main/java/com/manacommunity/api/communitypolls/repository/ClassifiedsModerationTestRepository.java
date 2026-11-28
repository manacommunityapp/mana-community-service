package com.manacommunity.api.communitypolls.repository;

import com.manacommunity.api.communitypolls.entity.ClassifiedsModerationTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassifiedsModerationTestRepository extends JpaRepository<ClassifiedsModerationTest, Long> {}
