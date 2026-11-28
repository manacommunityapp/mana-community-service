package com.manacommunity.api.whatsappbot.repository;

import com.manacommunity.api.whatsappbot.entity.BotSessionTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotSessionTestRepository extends JpaRepository<BotSessionTest, Long> {}
