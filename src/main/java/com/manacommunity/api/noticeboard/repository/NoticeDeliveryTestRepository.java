package com.manacommunity.api.noticeboard.repository;

import com.manacommunity.api.noticeboard.entity.NoticeDeliveryTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeDeliveryTestRepository extends JpaRepository<NoticeDeliveryTest, Long> {}
