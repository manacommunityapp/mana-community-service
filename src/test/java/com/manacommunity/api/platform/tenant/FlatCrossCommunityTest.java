package com.manacommunity.api.platform.tenant;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.resident.repository.CommunityFlatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that community_flat queries for community A return nothing when the
 * repository is called with community B's ID, ensuring no cross-community leakage.
 */
@DataJpaTest
class FlatCrossCommunityTest extends CrossCommunityTestBase {

    @Autowired
    private CommunityFlatRepository flatRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void communityB_cannotSee_communityA_flats() {
        // Arrange: persist a minimal Community A with one flat
        Community communityA = new Community();
        communityA.setId(COMMUNITY_A);
        em.persist(communityA);

        CommunityFlat flat = CommunityFlat.builder()
                .community(communityA)
                .towerBlock("A")
                .flatNumber("101")
                .build();
        em.persist(flat);
        em.flush();

        setTenantContext(COMMUNITY_B);

        // Act: query flats for community B
        List<CommunityFlat> results = flatRepository.findByCommunityId(COMMUNITY_B);

        // Assert: community B sees nothing of community A
        assertThat(results).isEmpty();
    }

    @Override
    protected void assertCommunityBCannotSeeA() {
        List<CommunityFlat> results = flatRepository.findByCommunityId(COMMUNITY_B);
        assertThat(results).isEmpty();
    }
}
