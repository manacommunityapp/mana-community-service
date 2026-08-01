package com.manacommunity.api.service;

import com.manacommunity.api.dto.EngagementScoreResponse;
import com.manacommunity.api.dto.FeedAnalyticsResponse;
import com.manacommunity.api.dto.TrendingResponse;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EngagementService {

    private final UserEngagementScoreRepository engagementScoreRepository;
    private final UserBadgeRepository badgeRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostReactionRepository reactionRepository;
    private final CommunityGroupRepository groupRepository;
    private final HashtagRepository hashtagRepository;
    private final TrendingTopicRepository trendingTopicRepository;
    private final ContentReportRepository contentReportRepository;

    @Transactional
    public void recordActivity(AppUser user, String activityType, int points) {
        if (user.getCommunity() == null) return;

        UserEngagementScore score = engagementScoreRepository
                .findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId())
                .orElseGet(() -> UserEngagementScore.builder()
                        .user(user)
                        .community(user.getCommunity())
                        .build());

        score.setTotalPoints(score.getTotalPoints() + points);

        switch (activityType) {
            case "POST" -> score.setPostsCount(score.getPostsCount() + 1);
            case "COMMENT" -> score.setCommentsCount(score.getCommentsCount() + 1);
            case "REACTION_GIVEN" -> score.setReactionsGiven(score.getReactionsGiven() + 1);
            case "REACTION_RECEIVED" -> score.setReactionsReceived(score.getReactionsReceived() + 1);
            case "GROUP_JOINED" -> score.setGroupsJoined(score.getGroupsJoined() + 1);
            case "HELPFUL" -> score.setHelpfulCount(score.getHelpfulCount() + 1);
            case "VOLUNTEER" -> score.setVolunteerPoints(score.getVolunteerPoints() + points);
        }

        int newLevel = calculateLevel(score.getTotalPoints());
        score.setLevel(newLevel);

        engagementScoreRepository.save(score);

        checkAndAwardBadges(user, score);
    }

    @Transactional(readOnly = true)
    public EngagementScoreResponse getEngagementScore(AppUser user) {
        if (user.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }

        UserEngagementScore score = engagementScoreRepository
                .findByUserIdAndCommunityId(user.getId(), user.getCommunity().getId())
                .orElse(UserEngagementScore.builder()
                        .user(user)
                        .community(user.getCommunity())
                        .build());

        List<UserBadge> badges = badgeRepository.findByUserIdAndCommunityIdOrderByEarnedAtDesc(
                user.getId(), user.getCommunity().getId());

        List<EngagementScoreResponse.BadgeResponse> badgeResponses = badges.stream()
                .map(b -> new EngagementScoreResponse.BadgeResponse(
                        b.getId(), b.getBadgeType(), b.getTitle(), b.getDescription(),
                        b.getIcon(), b.getPointsValue(), b.getEarnedAt().toString()))
                .toList();

        return new EngagementScoreResponse(
                user.getId(),
                user.getFullName(),
                user.getProfilePicUrl(),
                score.getTotalPoints(),
                score.getLevel(),
                score.getCurrentStreak(),
                score.getPostsCount(),
                score.getCommentsCount(),
                score.getReactionsReceived(),
                score.getHelpfulCount(),
                score.getVolunteerPoints(),
                score.getRankPosition(),
                badgeResponses
        );
    }

    @Transactional(readOnly = true)
    public List<EngagementScoreResponse> getLeaderboard(AppUser user, int limit) {
        if (user.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }

        List<UserEngagementScore> topScores = engagementScoreRepository
                .findTopContributors(user.getCommunity().getId(), PageRequest.of(0, limit));

        return topScores.stream().map(score -> {
            List<UserBadge> badges = badgeRepository.findByUserIdAndCommunityIdOrderByEarnedAtDesc(
                    score.getUser().getId(), score.getCommunity().getId());
            List<EngagementScoreResponse.BadgeResponse> badgeResponses = badges.stream()
                    .map(b -> new EngagementScoreResponse.BadgeResponse(
                            b.getId(), b.getBadgeType(), b.getTitle(), b.getDescription(),
                            b.getIcon(), b.getPointsValue(), b.getEarnedAt().toString()))
                    .toList();

            return new EngagementScoreResponse(
                    score.getUser().getId(),
                    score.getUser().getFullName(),
                    score.getUser().getProfilePicUrl(),
                    score.getTotalPoints(),
                    score.getLevel(),
                    score.getCurrentStreak(),
                    score.getPostsCount(),
                    score.getCommentsCount(),
                    score.getReactionsReceived(),
                    score.getHelpfulCount(),
                    score.getVolunteerPoints(),
                    score.getRankPosition(),
                    badgeResponses
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<TrendingResponse> getTrending(AppUser user, int limit) {
        if (user.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }

        List<Hashtag> hashtags = hashtagRepository.findTrendingHashtags(
                user.getCommunity().getId(), PageRequest.of(0, limit));

        if (hashtags.isEmpty()) {
            hashtags = hashtagRepository.findTopHashtags(
                    user.getCommunity().getId(), PageRequest.of(0, limit));
        }

        return hashtags.stream()
                .map(h -> new TrendingResponse(h.getId(), "#" + h.getName(), "HASHTAG", h.getPostCount(), h.getPostCount(), h.getPostCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedAnalyticsResponse getAnalytics(AppUser user) {
        if (user.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        Long communityId = user.getCommunity().getId();

        long totalPosts = postRepository.countByCommunityIdAndDeletedFalse(communityId);
        long totalGroups = groupRepository.countByCommunityIdAndActiveTrue(communityId);
        long pendingReports = contentReportRepository.countByCommunityIdAndStatus(communityId, "PENDING");

        List<Object[]> postsByType = postRepository.countPostsByType(communityId);
        Map<String, Long> postsByTypeMap = postsByType.stream()
                .collect(Collectors.toMap(
                        row -> ((PostType) row[0]).name(),
                        row -> (Long) row[1]
                ));

        List<TrendingResponse> trending = getTrending(user, 10);
        List<EngagementScoreResponse> topContributors = getLeaderboard(user, 10);

        double healthScore = Math.min(100.0, (totalPosts * 0.3 + totalGroups * 5 + topContributors.size() * 10));

        return new FeedAnalyticsResponse(
                totalPosts,
                0,
                0,
                totalGroups,
                topContributors.size(),
                healthScore,
                postsByTypeMap,
                Map.of(),
                trending,
                topContributors
        );
    }

    @Transactional
    public void reportContent(AppUser user, String contentType, Long contentId, String reason, String description) {
        if (user.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }

        if (contentReportRepository.existsByContentTypeAndContentIdAndReportedById(contentType, contentId, user.getId())) {
            throw new InvalidInputException("You have already reported this content.");
        }

        ContentReport report = ContentReport.builder()
                .community(user.getCommunity())
                .reportedBy(user)
                .contentType(contentType)
                .contentId(contentId)
                .reason(reason)
                .description(description)
                .build();
        contentReportRepository.save(report);
    }

    private int calculateLevel(int totalPoints) {
        if (totalPoints >= 10000) return 10;
        if (totalPoints >= 5000) return 9;
        if (totalPoints >= 2500) return 8;
        if (totalPoints >= 1500) return 7;
        if (totalPoints >= 1000) return 6;
        if (totalPoints >= 500) return 5;
        if (totalPoints >= 250) return 4;
        if (totalPoints >= 100) return 3;
        if (totalPoints >= 25) return 2;
        return 1;
    }

    private void checkAndAwardBadges(AppUser user, UserEngagementScore score) {
        Long communityId = user.getCommunity().getId();

        if (score.getPostsCount() >= 1 && !badgeRepository.existsByUserIdAndBadgeType(user.getId(), "FIRST_POST")) {
            awardBadge(user, communityId, "FIRST_POST", "First Post", "Posted your first community update", "✍️", 10);
        }
        if (score.getPostsCount() >= 10 && !badgeRepository.existsByUserIdAndBadgeType(user.getId(), "ACTIVE_POSTER")) {
            awardBadge(user, communityId, "ACTIVE_POSTER", "Active Poster", "Created 10 community posts", "📝", 25);
        }
        if (score.getCommentsCount() >= 10 && !badgeRepository.existsByUserIdAndBadgeType(user.getId(), "CONVERSATIONALIST")) {
            awardBadge(user, communityId, "CONVERSATIONALIST", "Conversationalist", "Left 10 comments", "💬", 25);
        }
        if (score.getHelpfulCount() >= 5 && !badgeRepository.existsByUserIdAndBadgeType(user.getId(), "HELPFUL_NEIGHBOR")) {
            awardBadge(user, communityId, "HELPFUL_NEIGHBOR", "Helpful Neighbor", "Received 5 helpful reactions", "🤝", 50);
        }
        if (score.getGroupsJoined() >= 3 && !badgeRepository.existsByUserIdAndBadgeType(user.getId(), "COMMUNITY_EXPLORER")) {
            awardBadge(user, communityId, "COMMUNITY_EXPLORER", "Community Explorer", "Joined 3 groups", "🧭", 30);
        }
        if (score.getTotalPoints() >= 500 && !badgeRepository.existsByUserIdAndBadgeType(user.getId(), "COMMUNITY_CHAMPION")) {
            awardBadge(user, communityId, "COMMUNITY_CHAMPION", "Community Champion", "Earned 500 engagement points", "🏆", 100);
        }
        if (score.getVolunteerPoints() >= 50 && !badgeRepository.existsByUserIdAndBadgeType(user.getId(), "VOLUNTEER_STAR")) {
            awardBadge(user, communityId, "VOLUNTEER_STAR", "Volunteer Star", "Active community volunteer", "⭐", 75);
        }
    }

    private void awardBadge(AppUser user, Long communityId, String type, String title, String desc, String icon, int points) {
        UserBadge badge = UserBadge.builder()
                .user(user)
                .community(Community.builder().id(communityId).build())
                .badgeType(type)
                .title(title)
                .description(desc)
                .icon(icon)
                .pointsValue(points)
                .build();
        badgeRepository.save(badge);
    }
}
