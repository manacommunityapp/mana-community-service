package com.manacommunity.api.service;

import com.manacommunity.api.user.model.AppUser;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.media.entity.MediaObject;
import com.manacommunity.api.media.repository.MediaRepository;
import com.manacommunity.api.media.service.MediaUrlService;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostPollVoteRepository pollVoteRepository;
    private final PostReactionRepository postReactionRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final HashtagRepository hashtagRepository;
    private final CommunityLeaderRepository communityLeaderRepository;
    private final CommunityWhoToCallRepository communityWhoToCallRepository;
    private final TrendingTopicRepository trendingTopicRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserEngagementScoreRepository userEngagementScoreRepository;
    private final SportsEventRepository sportsEventRepository;
    private final com.manacommunity.api.events.repository.EventCommunityRepository communityEventRepository;
    private final com.manacommunity.api.events.repository.EventRegistrationRepository eventRegistrationRepository;
    private final MediaRepository mediaRepository;
    private final MediaUrlService mediaUrlService;
    private final PostCommentLikeRepository postCommentLikeRepository;
    private final PostCommentReactionRepository postCommentReactionRepository;

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(AppUser currentUser, String type, int page, int size) {
        if (currentUser.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts;
        Long communityId = currentUser.getCommunity().getId();

        if (type == null || type.trim().isEmpty() || "ALL".equalsIgnoreCase(type)) {
            posts = postRepository.findByCommunityIdAndDeletedFalseOrderByPinnedDescCreatedAtDesc(communityId, pageable);
        } else if ("OFFICIAL".equalsIgnoreCase(type)) {
            posts = postRepository.findByCommunityIdAndOfficialTrueAndDeletedFalseOrderByCreatedAtDesc(communityId, pageable);
        } else if ("BOOKMARKED".equalsIgnoreCase(type)) {
            posts = postRepository.findBookmarkedByUser(currentUser.getId(), pageable);
        } else {
            try {
                PostType postType = PostType.valueOf(type.toUpperCase());
                posts = postRepository.findByCommunityIdAndPostTypeAndDeletedFalseOrderByCreatedAtDesc(communityId, postType, pageable);
            } catch (IllegalArgumentException e) {
                posts = postRepository.findByCommunityIdAndDeletedFalseOrderByPinnedDescCreatedAtDesc(communityId, pageable);
            }
        }
        return toPostResponsePage(posts, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getGroupFeed(AppUser currentUser, Long groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByGroupIdAndDeletedFalseOrderByPinnedDescCreatedAtDesc(groupId, pageable);
        return toPostResponsePage(posts, currentUser.getId());
    }

    @Transactional
    public PostResponse createPost(AppUser currentUser, PostRequest request) {
        if (currentUser.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        if (!canCreatePost(currentUser.getRole())) {
            throw new UnauthorizedActionException("Only admins and event admins can create posts in the community feed.");
        }
        boolean isOfficial = isAdminRole(currentUser.getRole());
        PostType type = request.type() != null ? request.type() : PostType.GENERAL;
        PostVisibility visibility = request.visibility() != null ? request.visibility() : PostVisibility.COMMUNITY;
        PostPriority priority = request.priority() != null ? request.priority() : PostPriority.NORMAL;

        if (type == PostType.EMERGENCY) {
            priority = PostPriority.EMERGENCY;
        }

        Post.PostBuilder builder = Post.builder()
                .user(currentUser)
                .community(currentUser.getCommunity())
                .content(request.content())
                .title(request.title())
                .imageUrl(request.imageUrl())
                .official(isOfficial)
                .postType(type)
                .visibility(visibility)
                .priority(priority)
                .price(request.price())
                .location(request.location())
                .pollQuestion(request.pollQuestion())
                .pollOptions(request.pollOptions())
                .pollEndDate(request.pollEndDate())
                .pollAnonymous(request.pollAnonymous() != null && request.pollAnonymous())
                .hashtags(request.hashtags())
                .mentions(request.mentions())
                .linkUrl(request.linkUrl())
                .linkTitle(request.linkTitle())
                .linkDescription(request.linkDescription())
                .linkImage(request.linkImage())
                .eventDate(request.eventDate())
                .eventEndDate(request.eventEndDate())
                .eventVenue(request.eventVenue());

        if (request.groupId() != null) {
            builder.group(CommunityGroup.builder().id(request.groupId()).build());
        }

        Post post = builder.build();
        Post savedPost = postRepository.save(post);

        if (request.mediaAttachments() != null && !request.mediaAttachments().isEmpty()) {
            int order = 0;
            for (PostRequest.MediaAttachment ma : request.mediaAttachments()) {
                UUID mediaObjectId = parseUuid(ma.mediaObjectId());
                // Verify media was confirmed in S3 before saving the post media record
                if (mediaObjectId != null) {
                    mediaRepository.findByExternalIdAndDeletedFalse(mediaObjectId)
                            .orElseThrow(() -> new InvalidInputException(
                                    "Media " + mediaObjectId + " was not found. Upload the file first."));
                }
                PostMedia media = PostMedia.builder()
                        .post(savedPost)
                        .mediaUrl(ma.mediaUrl())
                        .mediaType(ma.mediaType() != null ? ma.mediaType() : "IMAGE")
                        .thumbnailUrl(ma.thumbnailUrl())
                        .altText(ma.altText())
                        .sortOrder(ma.sortOrder() != null ? ma.sortOrder() : order++)
                        .mediaObjectExternalId(mediaObjectId)
                        .build();
                postMediaRepository.save(media);
            }
        }

        processHashtags(savedPost);

        return toPostResponse(savedPost, currentUser.getId());
    }

    @Transactional
    public void deletePost(AppUser currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        boolean isAdmin = isAdminRole(currentUser.getRole());
        if (!post.getUser().getId().equals(currentUser.getId()) && !isAdmin) {
            throw new UnauthorizedActionException("delete post " + postId);
        }
        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    private static final Set<String> EDIT_ALLOWED_ROLES = Set.of(
            "ADMIN", "SUPER_ADMIN", "COMMUNITY_ADMIN",
            "EVENTS_ADMIN", "EVENT_ADMIN", "SPORTS_ADMIN");

    private boolean canEditPost(String role) {
        if (role == null) return false;
        for (String r : role.split(",")) {
            if (EDIT_ALLOWED_ROLES.contains(r.trim().toUpperCase())) return true;
        }
        return false;
    }

    @Transactional
    public PostResponse updatePost(AppUser currentUser, Long postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        boolean isAuthor = post.getUser().getId().equals(currentUser.getId());
        boolean hasEditRole = canEditPost(currentUser.getRole());

        if (!isAuthor && !hasEditRole) {
            throw new UnauthorizedActionException("edit post " + postId);
        }

        if (request.content() != null && !request.content().isBlank()) {
            post.setContent(request.content().trim());
        }
        if (request.title() != null) {
            post.setTitle(request.title().isBlank() ? null : request.title().trim());
        }
        if (request.imageUrl() != null) {
            post.setImageUrl(request.imageUrl().isBlank() ? null : request.imageUrl().trim());
        }
        if (request.eventDate() != null)    post.setEventDate(request.eventDate());
        if (request.eventEndDate() != null) post.setEventEndDate(request.eventEndDate());
        if (request.eventVenue() != null) {
            post.setEventVenue(request.eventVenue().isBlank() ? null : request.eventVenue().trim());
        }
        if (request.location() != null) {
            post.setLocation(request.location().isBlank() ? null : request.location().trim());
        }
        if (request.price() != null) {
            post.setPrice(request.price());
        }

        Post saved = postRepository.save(post);

        if (request.mediaAttachments() != null) {
            postMediaRepository.deleteByPostId(saved.getId());
            int order = 0;
            for (UpdatePostRequest.MediaAttachment ma : request.mediaAttachments()) {
                if (ma.mediaUrl() == null || ma.mediaUrl().isBlank()) continue;
                UUID mediaObjectId = parseUuid(ma.mediaObjectId());
                // Verify media was confirmed in S3 before saving the updated post media record
                if (mediaObjectId != null) {
                    mediaRepository.findByExternalIdAndDeletedFalse(mediaObjectId)
                            .orElseThrow(() -> new InvalidInputException(
                                    "Media " + mediaObjectId + " was not found. Upload the file first."));
                }
                PostMedia pm = PostMedia.builder()
                        .post(saved)
                        .mediaUrl(ma.mediaUrl())
                        .mediaType(ma.mediaType() != null ? ma.mediaType() : "IMAGE")
                        .thumbnailUrl(ma.thumbnailUrl())
                        .altText(ma.altText())
                        .sortOrder(ma.sortOrder() != null ? ma.sortOrder() : order++)
                        .mediaObjectExternalId(mediaObjectId)
                        .build();
                postMediaRepository.save(pm);
            }
        }

        return toPostResponse(saved, currentUser.getId());
    }

    @Transactional
    public PostResponse pinPost(AppUser currentUser, Long postId) {
        if (!isAdminRole(currentUser.getRole())) {
            throw new UnauthorizedActionException("pin post " + postId);
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
        post.setPinned(!post.isPinned());
        postRepository.save(post);
        return toPostResponse(post, currentUser.getId());
    }

    @Transactional
    public ReactionResponse toggleReaction(AppUser currentUser, Long postId, ReactionType reactionType) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        Optional<PostReaction> existing = postReactionRepository.findByPostIdAndUserId(postId, currentUser.getId());
        if (existing.isPresent()) {
            PostReaction reaction = existing.get();
            if (reaction.getReactionType() == reactionType) {
                postReactionRepository.delete(reaction);
                post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            } else {
                reaction.setReactionType(reactionType);
                postReactionRepository.save(reaction);
            }
        } else {
            PostReaction reaction = PostReaction.builder()
                    .post(post)
                    .user(currentUser)
                    .reactionType(reactionType)
                    .build();
            postReactionRepository.save(reaction);
            post.setLikesCount(post.getLikesCount() + 1);
        }
        postRepository.save(post);

        Map<String, Long> counts = getReactionCounts(postId);
        Optional<ReactionType> userReaction = postReactionRepository.findReactionTypeByPostIdAndUserId(postId, currentUser.getId());
        int total = counts.values().stream().mapToInt(Long::intValue).sum();

        return new ReactionResponse(total, counts, userReaction.orElse(null));
    }

    @Transactional
    public LikeToggleResponse toggleLike(AppUser currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, currentUser.getId());
        boolean liked;
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            liked = false;
        } else {
            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(currentUser)
                    .build();
            postLikeRepository.save(newLike);
            post.setLikesCount(post.getLikesCount() + 1);
            liked = true;
        }
        postRepository.save(post);
        return new LikeToggleResponse(post.getLikesCount(), liked);
    }

    @Transactional
    public PostResponse toggleBookmark(AppUser currentUser, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        Optional<PostBookmark> existing = postBookmarkRepository.findByPostIdAndUserId(postId, currentUser.getId());
        if (existing.isPresent()) {
            postBookmarkRepository.delete(existing.get());
            post.setBookmarksCount(Math.max(0, post.getBookmarksCount() - 1));
        } else {
            PostBookmark bookmark = PostBookmark.builder()
                    .post(post)
                    .user(currentUser)
                    .build();
            postBookmarkRepository.save(bookmark);
            post.setBookmarksCount(post.getBookmarksCount() + 1);
        }
        postRepository.save(post);
        return toPostResponse(post, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getBookmarkedPosts(AppUser currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findBookmarkedByUser(currentUser.getId(), pageable);
        return toPostResponsePage(posts, currentUser.getId());
    }

    @Transactional
    public PostResponse voteOnPoll(AppUser currentUser, Long postId, String option) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        if (post.getPostType() != PostType.POLL) {
            throw new InvalidInputException("This post is not a poll.");
        }

        if (post.getPollEndDate() != null && LocalDateTime.now().isAfter(post.getPollEndDate())) {
            throw new InvalidInputException("This poll has ended.");
        }

        String optionsStr = post.getPollOptions();
        if (optionsStr == null || option == null) {
            throw new InvalidInputException("Invalid poll options.");
        }
        boolean isValidOption = false;
        for (String opt : optionsStr.split(",")) {
            if (opt.trim().equalsIgnoreCase(option.trim())) {
                isValidOption = true;
                break;
            }
        }
        if (!isValidOption) {
            throw new InvalidInputException("Option not found in this poll.");
        }

        Optional<PollVote> existingVote = pollVoteRepository.findByPostIdAndUserId(postId, currentUser.getId());
        if (existingVote.isPresent()) {
            PollVote vote = existingVote.get();
            vote.setSelectedOption(option);
            pollVoteRepository.save(vote);
        } else {
            PollVote vote = PollVote.builder()
                    .post(post)
                    .user(currentUser)
                    .selectedOption(option)
                    .build();
            pollVoteRepository.save(vote);
        }

        return toPostResponse(post, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public List<PostLikerResponse> getPostLikers(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", postId);
        }
        Map<Long, PostLikerResponse> likersMap = new LinkedHashMap<>();

        // 1. Reactions (LOVE, CELEBRATE, HELPFUL, etc.)
        List<PostReaction> reactions = postReactionRepository.findByPostId(postId);
        for (PostReaction r : reactions) {
            AppUser u = r.getUser();
            if (u != null) {
                likersMap.put(u.getId(), new PostLikerResponse(
                        u.getId(),
                        u.getFullName(),
                        u.getProfilePicUrl(),
                        mapRole(u),
                        r.getReactionType() != null ? r.getReactionType().name() : "LIKE",
                        r.getCreatedAt()
                ));
            }
        }

        // 2. Direct Likes (from post_like table)
        List<PostLike> likes = postLikeRepository.findByPostIdOrderByCreatedAtDesc(postId);
        for (PostLike l : likes) {
            AppUser u = l.getUser();
            if (u != null && !likersMap.containsKey(u.getId())) {
                likersMap.put(u.getId(), new PostLikerResponse(
                        u.getId(),
                        u.getFullName(),
                        u.getProfilePicUrl(),
                        mapRole(u),
                        "LIKE",
                        l.getCreatedAt()
                ));
            }
        }

        return new ArrayList<>(likersMap.values());
    }

    @Transactional
    public CommentLikeToggleResponse toggleCommentLike(AppUser currentUser, Long commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        Optional<PostCommentLike> existing = postCommentLikeRepository.findByCommentIdAndUserId(commentId, currentUser.getId());
        boolean liked;
        if (existing.isPresent()) {
            postCommentLikeRepository.delete(existing.get());
            comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
            liked = false;
        } else {
            PostCommentLike newLike = PostCommentLike.builder()
                    .comment(comment)
                    .user(currentUser)
                    .build();
            postCommentLikeRepository.save(newLike);
            comment.setLikesCount(comment.getLikesCount() + 1);
            liked = true;
        }
        postCommentRepository.save(comment);
        return new CommentLikeToggleResponse(commentId, comment.getLikesCount(), liked);
    }

    @Transactional(readOnly = true)
    public List<CommentLikerResponse> getCommentLikers(Long commentId) {
        if (!postCommentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment", commentId);
        }
        List<PostCommentLike> likes = postCommentLikeRepository.findByCommentIdOrderByCreatedAtDesc(commentId);
        return likes.stream()
                .filter(l -> l.getUser() != null)
                .map(l -> {
                    AppUser u = l.getUser();
                    return new CommentLikerResponse(
                            u.getId(),
                            u.getFullName(),
                            u.getProfilePicUrl(),
                            mapRole(u),
                            l.getCreatedAt()
                    );
                })
                .toList();
    }

    /**
     * Toggle a rich comment reaction (LIKE, LOVE, CELEBRATE, HELPFUL, THANKS).
     * - Same reaction → remove (un-react).
     * - Different reaction → change to new type.
     * - No existing reaction → add new.
     * Also syncs the comment's likesCount with the total reactions in post_comment_reaction.
     */
    @Transactional
    public CommentReactionToggleResponse toggleCommentReaction(AppUser currentUser, Long commentId, CommentReactionType reactionType) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        Optional<PostCommentReaction> existing =
                postCommentReactionRepository.findByCommentIdAndUserId(commentId, currentUser.getId());

        String userReaction = null;
        if (existing.isPresent()) {
            PostCommentReaction reaction = existing.get();
            if (reaction.getReactionType() == reactionType) {
                // Same → remove
                postCommentReactionRepository.delete(reaction);
            } else {
                // Different → change
                reaction.setReactionType(reactionType);
                postCommentReactionRepository.save(reaction);
                userReaction = reactionType.name();
            }
        } else {
            PostCommentReaction newReaction = PostCommentReaction.builder()
                    .comment(comment)
                    .user(currentUser)
                    .reactionType(reactionType)
                    .build();
            postCommentReactionRepository.save(newReaction);
            userReaction = reactionType.name();
        }

        // Sync likesCount to total reactions in the rich-reaction table
        Map<String, Long> counts = getCommentReactionCountsMap(commentId);
        int total = counts.values().stream().mapToInt(Long::intValue).sum();
        comment.setLikesCount(total);
        postCommentRepository.save(comment);

        return new CommentReactionToggleResponse(commentId, total, userReaction, counts);
    }

    /**
     * Returns per-type reaction counts for a given comment.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getCommentReactionCounts(Long commentId) {
        if (!postCommentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment", commentId);
        }
        return getCommentReactionCountsMap(commentId);
    }

    private Map<String, Long> getCommentReactionCountsMap(Long commentId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<Object[]> rows = postCommentReactionRepository.countReactionsByCommentId(commentId);
        for (Object[] row : rows) {
            CommentReactionType rt = (CommentReactionType) row[0];
            Long count = (Long) row[1];
            counts.put(rt.name(), count);
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId, Long currentUserId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", postId);
        }
        List<PostComment> allComments = postCommentRepository.findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId);

        List<PostComment> topLevel = allComments.stream()
                .filter(c -> c.getParent() == null)
                .toList();

        Map<Long, List<PostComment>> repliesMap = new HashMap<>();
        for (PostComment c : allComments) {
            if (c.getParent() != null) {
                repliesMap.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c);
            }
        }

        Set<Long> likedCommentIds = (currentUserId != null && !allComments.isEmpty())
                ? new HashSet<>(allComments.stream()
                .filter(c -> postCommentLikeRepository.existsByCommentIdAndUserId(c.getId(), currentUserId))
                .map(PostComment::getId)
                .toList())
                : Collections.emptySet();

        // Build per-comment rich reaction maps (user's reaction type + counts)
        Map<Long, String> userReactionPerComment = new HashMap<>();
        Map<Long, Map<String, Long>> reactionCountsPerComment = new HashMap<>();
        if (!allComments.isEmpty()) {
            for (PostComment c : allComments) {
                reactionCountsPerComment.put(c.getId(), getCommentReactionCountsMap(c.getId()));
                if (currentUserId != null) {
                    postCommentReactionRepository.findByCommentIdAndUserId(c.getId(), currentUserId)
                            .ifPresent(r -> userReactionPerComment.put(c.getId(), r.getReactionType().name()));
                }
            }
        }

        return topLevel.stream()
                .map(c -> toCommentResponseWithReplies(c, repliesMap, likedCommentIds,
                        userReactionPerComment, reactionCountsPerComment))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        return getComments(postId, null);
    }

    @Transactional
    public CommentResponse addComment(AppUser currentUser, Long postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        PostComment.PostCommentBuilder builder = PostComment.builder()
                .post(post)
                .user(currentUser)
                .content(request.content());

        if (request.parentId() != null) {
            PostComment parent = postCommentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", request.parentId()));
            builder.parent(parent);
            parent.setRepliesCount(parent.getRepliesCount() + 1);
            postCommentRepository.save(parent);
        }

        PostComment savedComment = postCommentRepository.save(builder.build());
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        return toCommentResponse(savedComment);
    }

    @Transactional
    public void deleteComment(AppUser currentUser, Long commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        boolean isAdmin = isAdminRole(currentUser.getRole());
        boolean isCommentAuthor = comment.getUser().getId().equals(currentUser.getId());
        boolean isPostAuthor = comment.getPost().getUser().getId().equals(currentUser.getId());

        if (!isCommentAuthor && !isPostAuthor && !isAdmin) {
            throw new UnauthorizedActionException("delete comment " + commentId);
        }

        Post post = comment.getPost();
        comment.setDeleted(true);
        postCommentRepository.save(comment);
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
        postRepository.save(post);
    }

    @Transactional
    public CommentResponse pinComment(AppUser currentUser, Long commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        boolean isPostAuthor = comment.getPost().getUser().getId().equals(currentUser.getId());
        if (!isPostAuthor && !isAdminRole(currentUser.getRole())) {
            throw new UnauthorizedActionException("pin comment " + commentId);
        }
        comment.setPinned(!comment.isPinned());
        postCommentRepository.save(comment);
        return toCommentResponse(comment);
    }

    @Transactional
    public CommentResponse markAcceptedAnswer(AppUser currentUser, Long commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        boolean isPostAuthor = comment.getPost().getUser().getId().equals(currentUser.getId());
        if (!isPostAuthor && !isAdminRole(currentUser.getRole())) {
            throw new UnauthorizedActionException("mark accepted answer " + commentId);
        }
        comment.setAcceptedAnswer(!comment.isAcceptedAnswer());
        postCommentRepository.save(comment);
        return toCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> searchPosts(AppUser currentUser, String query, int page, int size) {
        if (currentUser.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.searchPosts(currentUser.getCommunity().getId(), query, pageable);
        return toPostResponsePage(posts, currentUser.getId());
    }

    private Page<PostResponse> toPostResponsePage(Page<Post> postsPage, Long currentUserId) {
        List<Post> posts = postsPage.getContent();
        if (posts.isEmpty()) {
            return postsPage.map(p -> null);
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();

        // 1. Batch Likes for this user
        Set<Long> likedPostIds = (currentUserId != null)
                ? postLikeRepository.findLikedPostIdsByUserIdAndPostIdIn(currentUserId, postIds)
                : Collections.emptySet();

        // 2. Batch Bookmarks for this user
        Set<Long> bookmarkedPostIds = (currentUserId != null)
                ? postBookmarkRepository.findBookmarkedPostIdsByUserIdAndPostIdIn(currentUserId, postIds)
                : Collections.emptySet();

        // 3. Batch Reaction Counts: Map<PostId, Map<ReactionTypeStr, Count>>
        Map<Long, Map<String, Long>> reactionCountsByPostId = new HashMap<>();
        List<Object[]> rawReactionCounts = postReactionRepository.countReactionsByPostIdInGroupByReactionType(postIds);
        for (Object[] row : rawReactionCounts) {
            Long pid = (Long) row[0];
            ReactionType rt = (ReactionType) row[1];
            Long count = (Long) row[2];
            reactionCountsByPostId.computeIfAbsent(pid, k -> new LinkedHashMap<>()).put(rt.name(), count);
        }

        // 4. Batch User Reactions: Map<PostId, ReactionType>
        Map<Long, ReactionType> userReactionsByPostId = new HashMap<>();
        if (currentUserId != null) {
            List<Object[]> rawUserReactions = postReactionRepository.findUserReactionsByUserIdAndPostIdIn(currentUserId, postIds);
            for (Object[] row : rawUserReactions) {
                Long pid = (Long) row[0];
                ReactionType rt = (ReactionType) row[1];
                userReactionsByPostId.put(pid, rt);
            }
        }

        // 5. Batch Media Attachments: Map<PostId, List<PostMedia>>
        Map<Long, List<PostMedia>> mediaByPostId = new HashMap<>();
        List<PostMedia> allMedia = postMediaRepository.findByPostIdInOrderBySortOrderAsc(postIds);
        for (PostMedia pm : allMedia) {
            mediaByPostId.computeIfAbsent(pm.getPost().getId(), k -> new ArrayList<>()).add(pm);
        }

        // 6. Batch Polls (only for POLL posts)
        List<Long> pollPostIds = posts.stream()
                .filter(p -> p.getPostType() == PostType.POLL)
                .map(Post::getId)
                .toList();

        Map<Long, Map<String, Long>> pollVotesByPostId = new HashMap<>();
        Map<Long, String> userPollVotesByPostId = new HashMap<>();

        if (!pollPostIds.isEmpty()) {
            List<Object[]> aggregatedVotes = pollVoteRepository.countVotesByPostIdInGroupByOption(pollPostIds);
            for (Object[] row : aggregatedVotes) {
                Long pid = (Long) row[0];
                String opt = (String) row[1];
                Long count = (Long) row[2];
                pollVotesByPostId.computeIfAbsent(pid, k -> new HashMap<>()).put(opt, count);
            }

            if (currentUserId != null) {
                List<Object[]> userVotes = pollVoteRepository.findUserVotesByUserIdAndPostIdIn(currentUserId, pollPostIds);
                for (Object[] row : userVotes) {
                    Long pid = (Long) row[0];
                    String opt = (String) row[1];
                    userPollVotesByPostId.put(pid, opt);
                }
            }
        }

        return postsPage.map(post -> {
            Long pid = post.getId();
            AppUser author = post.getUser();
            String initials = author != null ? getInitials(author.getFullName()) : "??";
            boolean liked = likedPostIds.contains(pid);
            boolean bookmarked = bookmarkedPostIds.contains(pid);

            Map<String, Long> reactionCounts = reactionCountsByPostId.getOrDefault(pid, Collections.emptyMap());
            Optional<ReactionType> userReaction = Optional.ofNullable(userReactionsByPostId.get(pid));

            List<String> optionsList = null;
            Map<String, Long> pollVotes = null;
            String userVotedOption = null;

            if (post.getPostType() == PostType.POLL) {
                if (post.getPollOptions() != null) {
                    optionsList = Arrays.stream(post.getPollOptions().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                }

                pollVotes = new HashMap<>();
                if (optionsList != null) {
                    for (String opt : optionsList) {
                        pollVotes.put(opt, 0L);
                    }
                }
                Map<String, Long> actualVotes = pollVotesByPostId.get(pid);
                if (actualVotes != null) {
                    pollVotes.putAll(actualVotes);
                }
                userVotedOption = userPollVotesByPostId.get(pid);
            }

            List<PostMedia> mediaList = mediaByPostId.getOrDefault(pid, Collections.emptyList());
            List<PostResponse.MediaResponse> mediaResponses = mediaList.stream()
                    .map(m -> {
                        String url = m.getMediaUrl();
                        String thumbUrl = m.getThumbnailUrl();
                        if (m.getMediaObjectExternalId() != null) {
                            Optional<MediaObject> mo = mediaRepository.findByExternalIdAndDeletedFalse(m.getMediaObjectExternalId());
                            if (mo.isPresent()) {
                                url = mediaUrlService.generateUrl(mo.get());
                                String freshThumb = mediaUrlService.generateThumbnailUrl(mo.get());
                                if (freshThumb != null) thumbUrl = freshThumb;
                            }
                        }
                        String mediaObjectIdStr = m.getMediaObjectExternalId() != null ? m.getMediaObjectExternalId().toString() : null;
                        return new PostResponse.MediaResponse(m.getId(), url, m.getMediaType(), thumbUrl, m.getAltText(), m.getSortOrder(), mediaObjectIdStr);
                    })
                    .toList();

            PostResponse.GroupSummary groupSummary = null;
            if (post.getGroup() != null) {
                CommunityGroup g = post.getGroup();
                groupSummary = new PostResponse.GroupSummary(g.getId(), g.getName(), g.getSlug(), g.getIconUrl(), g.getGroupType());
            }

            return new PostResponse(
                    post.getId(),
                    post.getContent(),
                    post.getTitle(),
                    post.getImageUrl(),
                    post.isOfficial(),
                    post.isPinned(),
                    post.getLikesCount(),
                    post.getCommentsCount(),
                    post.getSharesCount(),
                    post.getBookmarksCount(),
                    post.getViewsCount(),
                    liked,
                    bookmarked,
                    userReaction.orElse(null),
                    reactionCounts,
                    author != null ? author.getId() : null,
                    author != null ? author.getFullName() : "",
                    initials,
                    author != null ? mapRole(author) : "",
                    author != null ? author.getProfilePicUrl() : null,
                    post.getCreatedAt(),
                    post.getPostType(),
                    post.getVisibility(),
                    post.getPriority(),
                    post.getPrice(),
                    post.getLocation(),
                    post.getPollQuestion(),
                    optionsList,
                    pollVotes,
                    userVotedOption,
                    post.getPollEndDate(),
                    post.isPollAnonymous(),
                    post.getHashtags(),
                    post.getMentions(),
                    post.getLinkUrl(),
                    post.getLinkTitle(),
                    post.getLinkDescription(),
                    post.getLinkImage(),
                    post.getEventDate(),
                    post.getEventEndDate(),
                    post.getEventVenue(),
                    mediaResponses,
                    groupSummary,
                    post.getModerationStatus()
            );
        });
    }

    private void processHashtags(Post post) {
        String content = post.getContent();
        if (content == null) return;

        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        Set<String> tags = new LinkedHashSet<>();
        while (matcher.find()) {
            tags.add(matcher.group(1).toLowerCase());
        }

        if (tags.isEmpty()) return;

        StringBuilder hashtagStr = new StringBuilder();
        for (String tag : tags) {
            Hashtag hashtag = hashtagRepository.findByNameAndCommunityId(tag, post.getCommunity().getId())
                    .orElseGet(() -> {
                        Hashtag h = Hashtag.builder()
                                .name(tag)
                                .community(post.getCommunity())
                                .build();
                        return hashtagRepository.save(h);
                    });
            hashtag.setPostCount(hashtag.getPostCount() + 1);
            hashtag.setLastUsedAt(LocalDateTime.now());
            hashtagRepository.save(hashtag);

            PostHashtag ph = PostHashtag.builder()
                    .post(post)
                    .hashtag(hashtag)
                    .build();
            postHashtagRepository.save(ph);

            if (!hashtagStr.isEmpty()) hashtagStr.append(",");
            hashtagStr.append(tag);
        }

        post.setHashtags(hashtagStr.toString());
        postRepository.save(post);
    }

    private Map<String, Long> getReactionCounts(Long postId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<Object[]> results = postReactionRepository.countReactionsByType(postId);
        for (Object[] row : results) {
            ReactionType rt = (ReactionType) row[0];
            Long count = (Long) row[1];
            counts.put(rt.name(), count);
        }
        return counts;
    }

    private PostResponse toPostResponse(Post post, Long currentUserId) {
        AppUser author = post.getUser();
        String initials = getInitials(author.getFullName());
        boolean liked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);
        boolean bookmarked = postBookmarkRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        Map<String, Long> reactionCounts = getReactionCounts(post.getId());
        Optional<ReactionType> userReaction = postReactionRepository.findReactionTypeByPostIdAndUserId(post.getId(), currentUserId);

        List<String> optionsList = null;
        Map<String, Long> pollVotes = null;
        String userVotedOption = null;

        if (post.getPostType() == PostType.POLL) {
            if (post.getPollOptions() != null) {
                optionsList = Arrays.stream(post.getPollOptions().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }

            pollVotes = new HashMap<>();
            if (optionsList != null) {
                for (String opt : optionsList) {
                    pollVotes.put(opt, 0L);
                }
            }

            List<Object[]> aggregatedVotes = pollVoteRepository.countVotesGroupByOption(post.getId());
            for (Object[] row : aggregatedVotes) {
                String opt = (String) row[0];
                Long count = (Long) row[1];
                pollVotes.put(opt, count);
            }

            Optional<PollVote> userVote = pollVoteRepository.findByPostIdAndUserId(post.getId(), currentUserId);
            if (userVote.isPresent()) {
                userVotedOption = userVote.get().getSelectedOption();
            }
        }

        List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderBySortOrderAsc(post.getId());
        List<PostResponse.MediaResponse> mediaResponses = mediaList.stream()
                .map(m -> {
                    String url = m.getMediaUrl();
                    String thumbUrl = m.getThumbnailUrl();
                    if (m.getMediaObjectExternalId() != null) {
                        Optional<MediaObject> mo = mediaRepository.findByExternalIdAndDeletedFalse(m.getMediaObjectExternalId());
                        if (mo.isPresent()) {
                            url = mediaUrlService.generateUrl(mo.get());
                            String freshThumb = mediaUrlService.generateThumbnailUrl(mo.get());
                            if (freshThumb != null) thumbUrl = freshThumb;
                        }
                    }
                    String mediaObjectIdStr = m.getMediaObjectExternalId() != null ? m.getMediaObjectExternalId().toString() : null;
                    return new PostResponse.MediaResponse(m.getId(), url, m.getMediaType(), thumbUrl, m.getAltText(), m.getSortOrder(), mediaObjectIdStr);
                })
                .toList();

        PostResponse.GroupSummary groupSummary = null;
        if (post.getGroup() != null) {
            CommunityGroup g = post.getGroup();
            groupSummary = new PostResponse.GroupSummary(g.getId(), g.getName(), g.getSlug(), g.getIconUrl(), g.getGroupType());
        }

        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getTitle(),
                post.getImageUrl(),
                post.isOfficial(),
                post.isPinned(),
                post.getLikesCount(),
                post.getCommentsCount(),
                post.getSharesCount(),
                post.getBookmarksCount(),
                post.getViewsCount(),
                liked,
                bookmarked,
                userReaction.orElse(null),
                reactionCounts,
                author.getId(),
                author.getFullName(),
                initials,
                mapRole(author),
                author.getProfilePicUrl(),
                post.getCreatedAt(),
                post.getPostType(),
                post.getVisibility(),
                post.getPriority(),
                post.getPrice(),
                post.getLocation(),
                post.getPollQuestion(),
                optionsList,
                pollVotes,
                userVotedOption,
                post.getPollEndDate(),
                post.isPollAnonymous(),
                post.getHashtags(),
                post.getMentions(),
                post.getLinkUrl(),
                post.getLinkTitle(),
                post.getLinkDescription(),
                post.getLinkImage(),
                post.getEventDate(),
                post.getEventEndDate(),
                post.getEventVenue(),
                mediaResponses,
                groupSummary,
                post.getModerationStatus()
        );
    }

    private CommentResponse toCommentResponse(PostComment comment) {
        AppUser author = comment.getUser();
        String initials = getInitials(author.getFullName());
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getContent(),
                author.getId(),
                author.getFullName(),
                initials,
                mapRole(author),
                author.getProfilePicUrl(),
                comment.getCreatedAt(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getLikesCount(),
                comment.getRepliesCount(),
                comment.isPinned(),
                comment.isAcceptedAnswer(),
                null
        );
    }

    private CommentResponse toCommentResponseWithReplies(PostComment comment, Map<Long, List<PostComment>> repliesMap, Set<Long> likedCommentIds) {
        return toCommentResponseWithReplies(comment, repliesMap, likedCommentIds,
                Collections.emptyMap(), Collections.emptyMap());
    }

    private CommentResponse toCommentResponseWithReplies(
            PostComment comment,
            Map<Long, List<PostComment>> repliesMap,
            Set<Long> likedCommentIds,
            Map<Long, String> userReactionPerComment,
            Map<Long, Map<String, Long>> reactionCountsPerComment) {

        AppUser author = comment.getUser();
        String initials = getInitials(author.getFullName());
        List<PostComment> replies = repliesMap.getOrDefault(comment.getId(), List.of());
        List<CommentResponse> replyResponses = replies.stream()
                .map(r -> toCommentResponseWithReplies(r, repliesMap, likedCommentIds,
                        userReactionPerComment, reactionCountsPerComment))
                .toList();

        boolean isLiked = likedCommentIds != null && likedCommentIds.contains(comment.getId());
        String userReaction = userReactionPerComment.get(comment.getId());
        Map<String, Long> reactionCounts = reactionCountsPerComment.getOrDefault(comment.getId(), Collections.emptyMap());

        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getContent(),
                author.getId(),
                author.getFullName(),
                initials,
                mapRole(author),
                author.getProfilePicUrl(),
                comment.getCreatedAt(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getLikesCount(),
                comment.getRepliesCount(),
                comment.isPinned(),
                comment.isAcceptedAnswer(),
                replyResponses,
                isLiked,
                userReaction,
                reactionCounts.isEmpty() ? null : reactionCounts
        );
    }

    private CommentResponse toCommentResponseWithReplies(PostComment comment, Map<Long, List<PostComment>> repliesMap) {
        return toCommentResponseWithReplies(comment, repliesMap, Collections.emptySet());
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException e) { return null; }
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "COMMUNITY_ADMIN");

    private static final Set<String> POST_ALLOWED_ROLES =
            Set.of("ADMIN", "SUPER_ADMIN", "COMMUNITY_ADMIN", "EVENT_ADMIN", "EVENTS_ADMIN");

    private boolean isAdminRole(String role) {
        return role != null && ADMIN_ROLES.contains(role.toUpperCase());
    }

    private boolean canCreatePost(String role) {
        if (role == null) return false;
        for (String r : role.split(",")) {
            if (POST_ALLOWED_ROLES.contains(r.trim().toUpperCase())) return true;
        }
        return false;
    }

    private String mapRole(AppUser user) {
        if (user == null) return "Verified Member";

        // 1. Check CommunityLeader table for active community post / designation
        if (communityLeaderRepository != null && user.getId() != null) {
            List<CommunityLeader> leaders = communityLeaderRepository.findByUserIdAndIsActiveTrue(user.getId());
            if (!leaders.isEmpty()) {
                CommunityLeader leader = leaders.get(0);
                if (leader.getDesignation() != null && !leader.getDesignation().isBlank()) {
                    String designation = leader.getDesignation().trim();
                    if (leader.getCommittee() != null && !leader.getCommittee().isBlank()
                            && !leader.getCommittee().trim().equalsIgnoreCase(designation)) {
                        return designation + " (" + leader.getCommittee().trim() + ")";
                    }
                    return designation;
                }
            }
        }

        // 2. Check user roles (e.g. Admin, Event Admin, Moderator, Committee Member, custom designation)
        String rawRole = user.getRole();
        if (rawRole != null && !rawRole.isBlank()) {
            String upper = rawRole.toUpperCase().trim();
            if (isAdminRole(upper)) return "Admin";
            if (upper.contains("EVENT_ADMIN") || upper.contains("EVENTS_ADMIN")) return "Event Admin";
            if (upper.contains("MODERATOR")) return "Moderator";
            if (upper.contains("PRESIDENT")) return "President";
            if (upper.contains("SECRETARY")) return "Secretary";
            if (upper.contains("TREASURER")) return "Treasurer";
            if (upper.contains("COMMITTEE")) return "Committee Member";
            if (!upper.equals("USER") && !upper.equals("MEMBER") && !upper.equals("ROLE_USER")) {
                return Arrays.stream(rawRole.split("[_,\\s]+"))
                        .filter(s -> !s.isBlank())
                        .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                        .reduce((a, b) -> a + " " + b)
                        .orElse("Verified Member");
            }
        }

        return "Verified Member";
    }

    @Transactional(readOnly = true)
    public FeedSummaryCountsResponse getSidebarSummaryCounts(AppUser currentUser) {
        if (currentUser == null || currentUser.getCommunity() == null) {
            return new FeedSummaryCountsResponse(0, 0, 0, 0, 0, 0, 0, 0, 1, 0);
        }
        Long communityId = currentUser.getCommunity().getId();
        Long userId = currentUser.getId();

        long dirLeaderCount = communityLeaderRepository.countByCommunityIdAndIsActiveTrue(communityId);
        long dirWhoToCallCount = communityWhoToCallRepository.countByCommunityIdAndIsActiveTrue(communityId);
        long directoryCount = dirLeaderCount + dirWhoToCallCount;

        long sportsCount = 0;
        try {
            sportsCount = sportsEventRepository.countByCommunityIdAndTournamentRegistrationStatusIn(
                    communityId,
                    List.of(
                            com.manacommunity.api.model.Tournament.EventStatus.REGISTRATION_OPEN,
                            com.manacommunity.api.model.Tournament.EventStatus.LIVE
                    )
            );
        } catch (Exception ignored) {
            sportsCount = sportsEventRepository.findByCommunityIdOrderByEventDateStartDesc(communityId).size();
        }

        long upcomingEvents = communityEventRepository.countUpcomingByCommunity(communityId);
        long myPasses = eventRegistrationRepository.countByUserId(userId);
        long trending = trendingTopicRepository.countByCommunityId(communityId);
        long myGroups = groupMembershipRepository.countByUserIdAndStatus(userId, "ACTIVE");
        long contributorsCount = Math.min(userEngagementScoreRepository.countByCommunityId(communityId), 5);

        Optional<UserEngagementScore> scoreOpt = userEngagementScoreRepository.findByUserIdAndCommunityId(userId, communityId);
        int points = scoreOpt.map(UserEngagementScore::getTotalPoints).orElse(0);
        int level = scoreOpt.map(UserEngagementScore::getLevel).orElse(1);

        long officialCount = postRepository.countByCommunityIdAndOfficialTrueAndDeletedFalse(communityId);

        return new FeedSummaryCountsResponse(
                directoryCount,
                sportsCount,
                upcomingEvents,
                myPasses,
                trending,
                myGroups,
                contributorsCount,
                points,
                level,
                officialCount
        );
    }
}
