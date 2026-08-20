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
    private final MediaRepository mediaRepository;
    private final MediaUrlService mediaUrlService;

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
        return posts.map(post -> toPostResponse(post, currentUser.getId()));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getGroupFeed(AppUser currentUser, Long groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByGroupIdAndDeletedFalseOrderByPinnedDescCreatedAtDesc(groupId, pageable);
        return posts.map(post -> toPostResponse(post, currentUser.getId()));
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
        Page<PostBookmark> bookmarks = postBookmarkRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        return bookmarks.map(bm -> toPostResponse(bm.getPost(), currentUser.getId()));
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
    public List<CommentResponse> getComments(Long postId) {
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

        return topLevel.stream()
                .map(c -> toCommentResponseWithReplies(c, repliesMap))
                .toList();
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
        return posts.map(post -> toPostResponse(post, currentUser.getId()));
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

    private CommentResponse toCommentResponseWithReplies(PostComment comment, Map<Long, List<PostComment>> repliesMap) {
        AppUser author = comment.getUser();
        String initials = getInitials(author.getFullName());
        List<PostComment> replies = repliesMap.getOrDefault(comment.getId(), List.of());
        List<CommentResponse> replyResponses = replies.stream()
                .map(r -> toCommentResponseWithReplies(r, repliesMap))
                .toList();

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
                replyResponses
        );
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
        if (communityLeaderRepository != null) {
            List<CommunityLeader> leaders = communityLeaderRepository.findByUserIdAndIsActiveTrue(user.getId());
            if (!leaders.isEmpty()) {
                CommunityLeader leader = leaders.get(0);
                if (leader.getCommittee() != null && !leader.getCommittee().isBlank()) {
                    return leader.getDesignation() + " (" + leader.getCommittee() + ")";
                }
                return leader.getDesignation();
            }
        }

        String rawRole = user.getRole();
        if (isAdminRole(rawRole)) return "Admin";
        if (rawRole != null && (rawRole.equalsIgnoreCase("EVENT_ADMIN") || rawRole.equalsIgnoreCase("EVENTS_ADMIN"))) return "Event Admin";
        if ("COMMITTEE".equalsIgnoreCase(rawRole)) return "Committee Member";
        return "Verified Member";
    }
}
