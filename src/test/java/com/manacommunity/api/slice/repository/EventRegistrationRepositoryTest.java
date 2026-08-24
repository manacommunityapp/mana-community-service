package com.manacommunity.api.slice.repository;

import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventRegistration;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventRegistrationRepository")
class EventRegistrationRepositoryTest extends BaseRepositoryTest {

    @Autowired EventRegistrationRepository regRepo;
    @Autowired TestEntityManager em;

    private Community savedCommunity;
    private AppUser savedUser;
    private EventCommunity savedEvent;
    private EventRegistration savedReg;

    @BeforeEach
    void setUp() {
        Community community = TestDataBuilder.community();
        community.setId(null);
        savedCommunity = em.persistAndFlush(community);

        AppUser user = TestDataBuilder.adminUser();
        user.setId(null);
        user.setCommunity(savedCommunity);
        savedUser = em.persistAndFlush(user);

        EventCommunity event = TestDataBuilder.communityEvent(null, savedCommunity, savedUser);
        event.setId(null);
        event.setStartDate(LocalDate.now().plusDays(10));
        savedEvent = em.persistAndFlush(event);

        EventRegistration reg = EventRegistration.builder()
                .event(savedEvent)
                .user(savedUser)
                .status(EventRegistration.RegistrationStatus.CONFIRMED)
                .registeredAt(LocalDateTime.now())
                .build();
        savedReg = em.persistAndFlush(reg);
        em.clear();
    }

    @Nested
    @DisplayName("Query Registration Methods")
    class RegistrationQueries {

        @Test
        @DisplayName("existsByEventIdAndUserId returns true when registered")
        void exists_true() {
            boolean exists = regRepo.existsByEventIdAndUserId(savedEvent.getId(), savedUser.getId());
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("findByEventIdAndUserId returns registration")
        void findByEventAndUser() {
            Optional<EventRegistration> reg = regRepo.findByEventIdAndUserId(savedEvent.getId(), savedUser.getId());
            assertThat(reg).isPresent();
            assertThat(reg.get().getId()).isEqualTo(savedReg.getId());
        }

        @Test
        @DisplayName("findByEventId returns all registrations for event")
        void findByEventId() {
            List<EventRegistration> list = regRepo.findByEventId(savedEvent.getId());
            assertThat(list).hasSize(1);
            assertThat(list.get(0).getId()).isEqualTo(savedReg.getId());
        }

        @Test
        @DisplayName("countByEventCommunityId returns total registrations count")
        void countByEventCommunityId() {
            long count = regRepo.countByEventCommunityId(savedCommunity.getId());
            assertThat(count).isEqualTo(1);
        }
    }
}
