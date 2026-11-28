package com.manacommunity.api.accesscontrol.repository;

import com.manacommunity.api.accesscontrol.entity.BiometricTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BiometricTestRepository extends JpaRepository<BiometricTest, Long> {}
