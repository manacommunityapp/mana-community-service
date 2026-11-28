package com.manacommunity.api.whatsappbot.repository;

import com.manacommunity.api.whatsappbot.entity.InteractiveButtonTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteractiveButtonTestRepository extends JpaRepository<InteractiveButtonTest, Long> {}
