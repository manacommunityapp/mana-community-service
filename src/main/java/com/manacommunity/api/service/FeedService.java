package com.manacommunity.api.service;

import com.manacommunity.api.user.model.AppUser;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.model.*;
import com.manacommunity.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostPollVoteRepository pollVoteRepository;
    private final CommunityLeaderRepository communityLeaderRepository;

    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(AppUser currentUser, String type, int page, int size) {
        if (currentUser.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts;
        if (type == null || type.trim().isEmpty() || "ALL".equalsIgnoreCase(type)) {
            posts = postRepository.findByCommunityIdOrderByCreatedAtDesc(currentUser.getCommunity().getId(), pageable);
        } else if ("OFFICIAL".equalsIgnoreCase(type)) {
            posts = postRepository.findByCommunityIdAndOfficialTrueOrderByCreatedAtDesc(currentUser.getCommunity().getId(), pageable);
        } else {
            try {
                PostType postType = PostType.valueOf(type.toUpperCase());
                posts = postRepository.findByCommunityIdAndPostTypeOrderByCreatedAtDesc(currentUser.getCommunity().getId(), postType, pageable);
            } catch (IllegalArgumentException e) {
                posts = postRepository.findByCommunityIdOrderByCreatedAtDesc(currentUser.getCommunity().getId(), pageable);
            }
        }
        return posts.map(post -> toPostResponse(post, currentUser.getId()));
    }

    @Transactional
    public PostResponse createPost(AppUser currentUser, PostRequest request) {
        if (currentUser.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        boolean isOfficial = "ADMIN".equalsIgnoreCase(currentUser.getRole()) 
                || "SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()) 
                || "COMMUNITY_ADMIN".equalsIgnoreCase(currentUser.getRole());
        PostType type = request.type() != null ? request.type() : PostType.GENERAL;

        Post post = Post.builder()
                .user(currentUser)
                .community(currentUser.getCommunity())
                .content(request.content())
                .imageUrl(request.imageUrl())
                .official(isOfficial)
                .postType(type)
                .price(request.price())
                .location(request.location())
                .pollQuestion(request.pollQuestion())
                .pollOptions(request.pollOptions())
                .likesCount(0)
                .commentsCount(0)
                .build();

        Post savedPost = postRepository.save(post);
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
        postRepository.delete(post);
    }

    @Transactional
    public PostResponse voteOnPoll(AppUser currentUser, Long postId, String option) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        if (post.getPostType() != PostType.POLL) {
            throw new InvalidInputException("This post is not a poll.");
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

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        // Just verify post exists
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", postId);
        }
        List<PostComment> comments = postCommentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return comments.stream().map(this::toCommentResponse).toList();
    }

    @Transactional
    public CommentResponse addComment(AppUser currentUser, Long postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        PostComment comment = PostComment.builder()
                .post(post)
                .user(currentUser)
                .content(request.content())
                .build();

        PostComment savedComment = postCommentRepository.save(comment);
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
        postCommentRepository.delete(comment);
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
        postRepository.save(post);
    }

    private PostResponse toPostResponse(Post post, Long currentUserId) {
        AppUser author = post.getUser();
        String initials = getInitials(author.getFullName());
        boolean liked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        List<String> optionsList = null;
        java.util.Map<String, Long> pollVotes = null;
        String userVotedOption = null;

        if (post.getPostType() == PostType.POLL) {
            if (post.getPollOptions() != null) {
                optionsList = java.util.Arrays.stream(post.getPollOptions().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }

            pollVotes = new java.util.HashMap<>();
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

        return new PostResponse(
                post.getId(),
                post.getContent(),
                post.getImageUrl(),
                post.isOfficial(),
                post.getLikesCount(),
                post.getCommentsCount(),
                liked,
                author.getId(),
                author.getFullName(),
                initials,
                mapRole(author),
                post.getCreatedAt(),
                post.getPostType(),
                post.getPrice(),
                post.getLocation(),
                post.getPollQuestion(),
                optionsList,
                pollVotes,
                userVotedOption
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
                comment.getCreatedAt()
        );
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "COMMUNITY_ADMIN");

    private boolean isAdminRole(String role) {
        return role != null && ADMIN_ROLES.contains(role.toUpperCase());
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
        if ("COMMITTEE".equalsIgnoreCase(rawRole)) return "Committee Member";
        return "Verified Member";
    }
}
