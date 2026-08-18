package com.manacommunity.api.slice.repository;

import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.support.BaseRepositoryTest;
import com.manacommunity.api.support.TestDataBuilder;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommunityEventRepository")
class CommunityEventRepositoryTest extends BaseRepositoryTest {

    @Autowired CommunityEventRepository eventRepo;
    @Autowired TestEntityManager em;

    private Community savedCommunity;
    private AppUser savedUser;
    private CommunityEvent savedEvent;

    @BeforeEach
    void setUp() {
        Community community = TestDataBuilder.community();
        community.setId(null);
        savedCommunity = em.persistAndFlush(community);

        AppUser user = TestDataBuilder.adminUser();
        user.setId(null);
        user.setCommunity(savedCommunity);
        savedUser = em.persistAndFlush(user);

        CommunityEvent event = TestDataBuilder.communityEvent(null, savedCommunity, savedUser);
        event.setId(null);
        event.setStartDate(LocalDate.now().plusDays(10));
        savedEvent = em.persistAndFlush(event);
        em.clear();
    }

    @Nested
    @DisplayName("Query Custom Methods")
    class CustomQueries {

        @Test
        @DisplayName("findByCommunityIdOrderByStartDateDesc returns events for community")
        void findByCommunityId() {
            List<CommunityEvent> events = eventRepo.findByCommunityIdOrderByStartDateDesc(savedCommunity.getId());

            assertThat(events).isNotEmpty();
            assertThat(events.get(0).getTitle()).isEqualTo(savedEvent.getTitle());
        }

        @Test
        @DisplayName("findUpcomingByCommunity returns upcoming events")
        void findUpcoming() {
            List<CommunityEvent> upcoming = eventRepo.findUpcomingByCommunity(savedCommunity.getId());

            assertThat(upcoming).isNotEmpty();
            assertThat(upcoming.get(0).getId()).isEqualTo(savedEvent.getId());
        }

        @Test
        @DisplayName("countByCommunityId returns count of events")
        void countByCommunityId() {
            long count = eventRepo.countByCommunityId(savedCommunity.getId());

            assertThat(count).isEqualTo(1);
        }
    }
}
