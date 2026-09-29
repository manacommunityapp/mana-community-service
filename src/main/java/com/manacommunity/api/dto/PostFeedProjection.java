package com.manacommunity.api.dto;

import com.manacommunity.api.model.PostPriority;
import com.manacommunity.api.model.PostType;
import com.manacommunity.api.model.PostVisibility;

import java.time.LocalDateTime;

/**
 * Lightweight projection for Community Feed post retrieval.
 * Transmits ONLY the fields needed for the feed card display and eliminates
 * heavy AppUser entity graphs, sensitive columns (passwords, KYC, DOB, phone),
 * and N+1 cascading relations.
 */
public interface PostFeedProjection {
    Long getId();
    String getContent();
    String getTitle();
    String getImageUrl();
    boolean isOfficial();
    boolean isPinned();
    int getLikesCount();
    int getCommentsCount();
    int getSharesCount();
    int getBookmarksCount();
    int getViewsCount();
    LocalDateTime getCreatedAt();
    PostType getPostType();
    PostVisibility getVisibility();
    PostPriority getPriority();
    Double getPrice();
    String getLocation();
    String getPollQuestion();
    String getPollOptions();
    LocalDateTime getPollEndDate();
    boolean isPollAnonymous();
    String getHashtags();
    String getMentions();
    String getLinkUrl();
    String getLinkTitle();
    String getLinkDescription();
    String getLinkImage();
    LocalDateTime getEventDate();
    LocalDateTime getEventEndDate();
    String getEventVenue();
    String getModerationStatus();

    // Public Author fields only (No PII / passwords / KYC)
    Long getAuthorId();
    String getAuthorFullName();
    String getAuthorRole();
    String getAuthorProfilePicUrl();

    // Group fields
    Long getGroupId();
    String getGroupName();
    String getGroupSlug();
    String getGroupIconUrl();
    String getGroupType();
}
