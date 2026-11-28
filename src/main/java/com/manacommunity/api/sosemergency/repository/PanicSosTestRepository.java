package com.manacommunity.api.sosemergency.repository;

import com.manacommunity.api.sosemergency.entity.PanicSosTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PanicSosTestRepository extends JpaRepository<PanicSosTest, Long> {}
