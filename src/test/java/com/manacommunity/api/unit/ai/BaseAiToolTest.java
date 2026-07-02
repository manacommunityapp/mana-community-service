package com.manacommunity.api.unit.ai;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Base class for AI tool unit tests.
 *
 * <p>Sets up a mock {@link EntityManager} and pre-configures the
 * {@link AgentSecurityContext} with a standard test user. Subclasses
 * can override {@link #defaultUserContext()} for admin/member variations.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @ExtendWith(MockitoExtension.class)
 * class AuctionQueryToolsTest extends BaseAiToolTest {
 *     @InjectMocks AuctionQueryTools tools;
 *
 *     @Test void searchPlayers_deniesWrongCommunity() { ... }
 * }
 * }</pre>
 */
public abstract class BaseAiToolTest {

    @Mock
    protected EntityManager em;

    protected static final Long TEST_USER_ID = 1L;
    protected static final Long TEST_COMMUNITY_ID = 100L;
    protected static final Long TEST_TEAM_ID = 10L;

    @BeforeEach
    void setUpSecurityContext() {
        AgentSecurityContext.set(defaultUserContext());
    }

    @AfterEach
    void clearSecurityContext() {
        AgentSecurityContext.clear();
    }

    /**
     * Override in subclasses to test with different roles.
     */
    protected UserContext defaultUserContext() {
        return new UserContext(
                TEST_USER_ID,
                TEST_COMMUNITY_ID,
                "MEMBER",
                TEST_TEAM_ID,
                "Test User",
                true);
    }

    protected UserContext adminContext() {
        return new UserContext(
                TEST_USER_ID,
                TEST_COMMUNITY_ID,
                "SUPER_ADMIN",
                TEST_TEAM_ID,
                "Admin User",
                true);
    }

    protected UserContext memberContext() {
        return defaultUserContext();
    }

    /**
     * Helper to mock a TypedQuery that returns a single Long result.
     */
    @SuppressWarnings("unchecked")
    protected TypedQuery<Long> mockCountQuery(Long result) {
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(result);
        return query;
    }

    /**
     * Helper to mock a TypedQuery that returns a list of Object[] results.
     */
    @SuppressWarnings("unchecked")
    protected TypedQuery<Object[]> mockListQuery(java.util.List<Object[]> results) {
        TypedQuery<Object[]> query = mock(TypedQuery.class);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(results);
        return query;
    }
}
