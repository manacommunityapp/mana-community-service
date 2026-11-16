package com.manacommunity.api.socialcircle.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class KittyPartyPostDtos {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePostRequest {
        @Builder.Default
        private String postType = "TEXT"; // TEXT, ANNOUNCEMENT, PHOTO, POLL

        private String title;

        @NotBlank(message = "Content cannot be empty")
        private String content;

        private String attachmentUrl;

        @Builder.Default
        private Boolean isPinned = false;

        private List<String> pollOptions;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePostRequest {
        private String title;
        private String content;
        private String attachmentUrl;
        private Boolean isPinned;
        private String status;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PollOptionResponse {
        private Long id;
        private String optionText;
        private long voteCount;
        private double percentage;
        private boolean userVoted;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CastVoteRequest {
        private Long optionId;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostResponse {
        private Long id;
        private Long circleId;
        private String circleName;
        private Long authorId;
        private String authorName;
        private String postType;
        private String title;
        private String content;
        private String attachmentUrl;
        private boolean isPinned;
        private String status;
        private long commentsCount;
        private List<PollOptionResponse> pollOptions;
        private Long userVotedOptionId;
        private long totalVotes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCommentRequest {
        @NotBlank(message = "Comment text cannot be empty")
        private String commentText;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentResponse {
        private Long id;
        private Long postId;
        private Long userId;
        private String userName;
        private String commentText;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostDetailResponse {
        private PostResponse post;
        private List<CommentResponse> comments;
    }
}
