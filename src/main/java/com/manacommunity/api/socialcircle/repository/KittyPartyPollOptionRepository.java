package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyPollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KittyPartyPollOptionRepository extends JpaRepository<KittyPartyPollOption, Long> {

    List<KittyPartyPollOption> findByPostIdOrderBySortOrderAsc(Long postId);

    void deleteByPostId(Long postId);
}
