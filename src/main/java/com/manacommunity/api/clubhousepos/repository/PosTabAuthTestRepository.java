package com.manacommunity.api.clubhousepos.repository;

import com.manacommunity.api.clubhousepos.entity.PosTabAuthTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PosTabAuthTestRepository extends JpaRepository<PosTabAuthTest, Long> {}
