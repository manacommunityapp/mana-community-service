package com.manacommunity.api.socialcircle.service;

import com.manacommunity.api.socialcircle.dto.KittyPartyPostDtos.*;
import com.manacommunity.api.socialcircle.entity.KittyPartyCircle;
import com.manacommunity.api.socialcircle.entity.KittyPartyPollOption;
import com.manacommunity.api.socialcircle.entity.KittyPartyPollVote;
import com.manacommunity.api.socialcircle.entity.KittyPartyPost;
import com.manacommunity.api.socialcircle.entity.KittyPartyPostComment;
import com.manacommunity.api.socialcircle.repository.*;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KittyPartyPostService {

    private final KittyPartyPostRepository postRepository;
    private final KittyPartyPostCommentRepository commentRepository;
    private final KittyPartyCircleRepository circleRepository;
    private final KittyPartyMemberRepository memberRepository;
    private final KittyPartyPollOptionRepository pollOptionRepository;
    private final KittyPartyPollVoteRepository pollVoteRepository;
    private final KittyPartyAuthorizationService authorizationService;

    @Transactional
    public PostResponse createPost(AppUser author, Long circleId, CreatePostRequest request) {
        authorizationService.assertCircleMember(circleId, author.getId());

        KittyPartyCircle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new IllegalArgumentException("Circle not found with id: " + circleId));

        boolean pin = Boolean.TRUE.equals(request.getIsPinned());
        boolean hasPoll = request.getPollOptions() != null && !request.getPollOptions().isEmpty();
        String pType = hasPoll ? "POLL" : (request.getPostType() != null ? request.getPostType() : "TEXT");

        KittyPartyPost post = KittyPartyPost.builder()
                .circle(circle)
                .author(author)
                .postType(pType)
                .title(request.getTitle())
                .content(request.getContent().trim())
                .attachmentUrl(request.getAttachmentUrl())
                .isPinned(pin)
                .status(pin ? "PINNED" : "PUBLISHED")
                .build();

        KittyPartyPost saved = postRepository.save(post);

        if (hasPoll) {
            int sortOrder = 0;
            for (String optText : request.getPollOptions()) {
                if (optText != null && !optText.trim().isEmpty()) {
                    pollOptionRepository.save(KittyPartyPollOption.builder()
                            .post(saved)
                            .optionText(optText.trim())
                            .sortOrder(sortOrder++)
                            .build());
                }
            }
        }

        log.info("Created kitty party post id={} circleId={} authorId={}", saved.getId(), circleId, author != null ? author.getId() : null);
        return toPostResponse(saved, author != null ? author.getId() : null);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByCircle(Long circleId) {
        return getPostsByCircle(circleId, null);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByCircle(Long circleId, Long currentUserId) {
        return postRepository.findByCircleIdOrderByPinnedAndCreatedAt(circleId).stream()
                .map(p -> toPostResponse(p, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long postId) {
        KittyPartyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));

        List<CommentResponse> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());

        return PostDetailResponse.builder()
                .post(toPostResponse(post))
                .comments(comments)
                .build();
    }

    @Transactional
    public PostResponse updatePost(AppUser actor, Long postId, UpdatePostRequest request) {
        if (actor == null) {
            throw new SecurityException("Authentication required to update posts");
        }
        KittyPartyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));

        boolean isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(actor.getId());
        if (!isAuthor) {
            authorizationService.assertAdminOrOwner(post.getCircle().getId(), actor.getId());
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            post.setContent(request.getContent().trim());
        }
        if (request.getAttachmentUrl() != null) {
            post.setAttachmentUrl(request.getAttachmentUrl());
        }
        if (request.getIsPinned() != null) {
            post.setIsPinned(request.getIsPinned());
            post.setStatus(post.getIsPinned() ? "PINNED" : "PUBLISHED");
        }
        if (request.getStatus() != null) {
            post.setStatus(request.getStatus());
        }
        post.setUpdatedAt(LocalDateTime.now());

        KittyPartyPost updated = postRepository.save(post);
        log.info("Updated kitty party post id={}", postId);
        return toPostResponse(updated);
    }

    @Transactional
    public PostResponse togglePinPost(AppUser actor, Long postId) {
        KittyPartyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
        authorizationService.assertAdminOrOwner(post.getCircle().getId(), actor.getId());

        boolean newPin = !Boolean.TRUE.equals(post.getIsPinned());
        post.setIsPinned(newPin);
        post.setStatus(newPin ? "PINNED" : "PUBLISHED");
        post.setUpdatedAt(LocalDateTime.now());

        KittyPartyPost updated = postRepository.save(post);
        log.info("Toggled pin status to {} for post id={}", newPin, postId);
        return toPostResponse(updated);
    }

    @Transactional
    public void deletePost(AppUser actor, Long postId) {
        if (actor == null) {
            throw new SecurityException("Authentication required to delete posts");
        }
        KittyPartyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));

        boolean isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(actor.getId());
        if (!isAuthor) {
            authorizationService.assertAdminOrOwner(post.getCircle().getId(), actor.getId());
        }

        post.setStatus("DELETED");
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);
        log.info("Marked post id={} as DELETED by user id={}", postId, actor != null ? actor.getId() : null);
    }

    @Transactional
    public CommentResponse addComment(AppUser user, Long postId, CreateCommentRequest request) {
        KittyPartyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));

        KittyPartyPostComment comment = KittyPartyPostComment.builder()
                .post(post)
                .user(user)
                .commentText(request.getCommentText().trim())
                .build();

        KittyPartyPostComment saved = commentRepository.save(comment);
        log.info("Added comment id={} to post id={} by user id={}", saved.getId(), postId, user.getId());
        return toCommentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(AppUser actor, Long commentId) {
        if (actor == null) {
            throw new SecurityException("Authentication required to delete comments");
        }
        KittyPartyPostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + commentId));

        boolean isAuthor = comment.getUser() != null && comment.getUser().getId().equals(actor.getId());
        if (!isAuthor) {
            authorizationService.assertAdminOrOwner(comment.getPost().getCircle().getId(), actor.getId());
        }

        commentRepository.delete(comment);
        log.info("Deleted comment id={} by user id={}", commentId, actor.getId());
    }

    @Transactional
    public PostResponse castVote(AppUser voter, Long postId, Long optionId) {
        KittyPartyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
        authorizationService.assertCircleMember(post.getCircle().getId(), voter.getId());

        KittyPartyPollOption option = pollOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Option not found with id: " + optionId));

        if (!option.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("Option does not belong to this poll");
        }

        if (pollVoteRepository.existsByPostIdAndUserId(postId, voter.getId())) {
            throw new IllegalStateException("You have already voted on this poll");
        }

        KittyPartyPollVote vote = KittyPartyPollVote.builder()
                .post(post)
                .option(option)
                .user(voter)
                .build();
        pollVoteRepository.save(vote);
        log.info("User id={} voted for option id={} on post id={}", voter.getId(), optionId, postId);

        return toPostResponse(post, voter.getId());
    }

    private PostResponse toPostResponse(KittyPartyPost p) {
        return toPostResponse(p, null);
    }

    private PostResponse toPostResponse(KittyPartyPost p, Long currentUserId) {
        long count = commentRepository.countByPostId(p.getId());

        List<PollOptionResponse> pollOptionResponses = null;
        Long userVotedOptionId = null;
        long totalVotes = 0;

        if ("POLL".equalsIgnoreCase(p.getPostType())) {
            List<KittyPartyPollOption> options = pollOptionRepository.findByPostIdOrderBySortOrderAsc(p.getId());
            totalVotes = pollVoteRepository.countByPostId(p.getId());

            if (currentUserId != null) {
                var userVote = pollVoteRepository.findByPostIdAndUserId(p.getId(), currentUserId);
                if (userVote.isPresent()) {
                    userVotedOptionId = userVote.get().getOption().getId();
                }
            }

            pollOptionResponses = new ArrayList<>();
            for (KittyPartyPollOption opt : options) {
                long optVotes = pollVoteRepository.countByOptionId(opt.getId());
                double pct = totalVotes > 0 ? (optVotes * 100.0) / totalVotes : 0.0;
                boolean userVotedThis = userVotedOptionId != null && userVotedOptionId.equals(opt.getId());

                pollOptionResponses.add(PollOptionResponse.builder()
                        .id(opt.getId())
                        .optionText(opt.getOptionText())
                        .voteCount(optVotes)
                        .percentage(Math.round(pct * 10.0) / 10.0)
                        .userVoted(userVotedThis)
                        .build());
            }
        }

        return PostResponse.builder()
                .id(p.getId())
                .circleId(p.getCircle().getId())
                .circleName(p.getCircle().getCircleName())
                .authorId(p.getAuthor() != null ? p.getAuthor().getId() : null)
                .authorName(p.getAuthor() != null ? p.getAuthor().getFullName() : null)
                .postType(p.getPostType())
                .title(p.getTitle())
                .content(p.getContent())
                .attachmentUrl(p.getAttachmentUrl())
                .isPinned(Boolean.TRUE.equals(p.getIsPinned()))
                .status(p.getStatus())
                .commentsCount(count)
                .pollOptions(pollOptionResponses)
                .userVotedOptionId(userVotedOptionId)
                .totalVotes(totalVotes)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private CommentResponse toCommentResponse(KittyPartyPostComment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .postId(c.getPost().getId())
                .userId(c.getUser() != null ? c.getUser().getId() : null)
                .userName(c.getUser() != null ? c.getUser().getFullName() : null)
                .commentText(c.getCommentText())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
