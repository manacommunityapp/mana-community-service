package com.manacommunity.api.megatownship.repository;

import com.manacommunity.api.megatownship.entity.SharedAmenityTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedAmenityTestRepository extends JpaRepository<SharedAmenityTest, Long> {}
