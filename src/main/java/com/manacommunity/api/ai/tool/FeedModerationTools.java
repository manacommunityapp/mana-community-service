package com.manacommunity.api.ai.tool;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI tools for community feed management and content moderation.
 *
 * <p>Members can view feed stats and their own posts. Admins can review
 * recent posts, flag/remove inappropriate content, and get feed analytics.</p>
 */
@Slf4j
@Component
@Transactional(readOnly = true)
public class FeedModerationTools {

    @PersistenceContext
    private EntityManager em;

    @Tool(description = "Get community feed stats — total posts, comments, likes, most active "
            + "posters, and posts per day trend. Community-scoped, read-only.")
    public Object getFeedAnalytics() {
        UserContext ctx = AgentSecurityContext.get();

        Map<String, Object> result = new LinkedHashMap<>();

        Long totalPosts = em.createQuery(
                "SELECT COUNT(p) FROM Post p WHERE p.community.id = :comId", Long.class)
                .setParameter("comId", ctx.communityId()).getSingleResult();
        result.put("total_posts", totalPosts);

        Long totalComments = em.createQuery(
                "SELECT SUM(p.commentsCount) FROM Post p WHERE p.community.id = :comId", Long.class)
                .setParameter("comId", ctx.communityId()).getSingleResult();
        result.put("total_comments", totalComments != null ? totalComments : 0);

        Long totalLikes = em.createQuery(
                "SELECT SUM(p.likesCount) FROM Post p WHERE p.community.id = :comId", Long.class)
                .setParameter("comId", ctx.communityId()).getSingleResult();
        result.put("total_likes", totalLikes != null ? totalLikes : 0);

        // Most active posters
        var topPosters = em.createQuery(
                "SELECT u.fullName, COUNT(p) FROM Post p JOIN p.user u " +
                "WHERE p.community.id = :comId GROUP BY u.fullName " +
                "ORDER BY COUNT(p) DESC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .setMaxResults(5)
                .getResultList();
        result.put("top_posters", topPosters.stream()
                .map(r -> Map.of("name", r[0], "post_count", r[1]))
                .collect(Collectors.toList()));

        // Most engaging posts (by likes + comments)
        var topPosts = em.createQuery(
                "SELECT p.id, SUBSTRING(p.content, 1, 80), u.fullName, " +
                "p.likesCount, p.commentsCount, p.createdAt " +
                "FROM Post p JOIN p.user u WHERE p.community.id = :comId " +
                "ORDER BY (p.likesCount + p.commentsCount) DESC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .setMaxResults(5)
                .getResultList();
        result.put("most_engaging_posts", topPosts.stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("post_id", r[0]);
                    m.put("preview", r[1]);
                    m.put("author", r[2]);
                    m.put("likes", r[3]);
                    m.put("comments", r[4]);
                    m.put("created_at", r[5] != null ? r[5].toString() : null);
                    return m;
                })
                .collect(Collectors.toList()));

        return result;
    }

    @Tool(description = "[ADMIN] Review recent posts in the community feed — shows content, "
            + "author, likes, comments, and whether it's an official post. "
            + "Use to identify content that may need moderation.")
    public Object reviewRecentPosts(
            @ToolParam(required = false, description = "Max results (default 15)") Integer limit,
            @ToolParam(required = false, description = "Filter by author name (partial match)")
            String authorName) {

        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isAdmin()) {
            return Map.of("error", "Admin access required for content review.");
        }

        StringBuilder jpql = new StringBuilder(
                "SELECT p.id, p.content, u.fullName, u.role, p.official, " +
                "p.likesCount, p.commentsCount, p.imageUrl, p.createdAt " +
                "FROM Post p JOIN p.user u WHERE p.community.id = :comId");
        if (authorName != null) {
            jpql.append(" AND LOWER(u.fullName) LIKE LOWER(:an)");
        }
        jpql.append(" ORDER BY p.createdAt DESC");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("comId", ctx.communityId());
        if (authorName != null) query.setParameter("an", "%" + authorName + "%");
        query.setMaxResults(limit != null ? limit : 15);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("post_id", r[0]);
                    m.put("content", r[1]);
                    m.put("author", r[2]);
                    m.put("author_role", r[3]);
                    m.put("is_official", r[4]);
                    m.put("likes", r[5]);
                    m.put("comments", r[6]);
                    m.put("has_image", r[7] != null);
                    m.put("created_at", r[8] != null ? r[8].toString() : null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "[ADMIN] Remove a post from the feed. This deletes the post and its "
            + "comments. Requires explicit confirmation. Use after reviewing content.")
    @Transactional
    public Object removePost(
            @ToolParam(description = "Post ID to remove") Long postId,
            @ToolParam(required = false, description = "Reason for removal") String reason,
            @ToolParam(description = "Has the user explicitly confirmed?") Boolean confirmed) {

        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isAdmin()) {
            return Map.of("error", "Admin access required.");
        }

        var rows = em.createQuery(
                "SELECT p.id, SUBSTRING(p.content, 1, 100), u.fullName " +
                "FROM Post p JOIN p.user u " +
                "WHERE p.id = :pid AND p.community.id = :comId", Object[].class)
                .setParameter("pid", postId)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        if (rows.isEmpty()) return Map.of("error", "Post not found in your community");

        if (!Boolean.TRUE.equals(confirmed)) {
            return Map.of("requires_confirmation", true,
                    "post_preview", rows.get(0)[1],
                    "author", rows.get(0)[2],
                    "message", "Remove this post by " + rows.get(0)[2] + "?");
        }

        // Delete comments first, then the post
        em.createQuery("DELETE FROM PostComment c WHERE c.post.id = :pid")
                .setParameter("pid", postId).executeUpdate();
        em.createQuery("DELETE FROM PostLike l WHERE l.post.id = :pid")
                .setParameter("pid", postId).executeUpdate();
        em.createQuery("DELETE FROM Post p WHERE p.id = :pid")
                .setParameter("pid", postId).executeUpdate();

        log.info("Post {} removed by admin user={}, reason={}", postId, ctx.userId(), reason);
        return Map.of("success", true, "message", "Post removed.",
                "reason", reason != null ? reason : "Admin moderation");
    }
}
