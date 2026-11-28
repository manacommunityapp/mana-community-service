package com.manacommunity.api.noticeboard.repository;

import com.manacommunity.api.noticeboard.entity.DigitalSignageTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DigitalSignageTestRepository extends JpaRepository<DigitalSignageTest, Long> {}
