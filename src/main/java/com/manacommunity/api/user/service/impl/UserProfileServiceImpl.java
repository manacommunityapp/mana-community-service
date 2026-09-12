package com.manacommunity.api.user.service.impl;

import com.manacommunity.api.model.Community;

import com.manacommunity.api.user.dto.UserProfileRequest;
import com.manacommunity.api.user.dto.UserProfileResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.UserProfile;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.repository.UserProfileRepository;
import com.manacommunity.api.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AppUserRepository appUserRepository;
    private final com.manacommunity.api.repository.SportsEventRegistrationRepository sportsEventRegistrationRepository;
    private final com.manacommunity.api.repository.PostRepository postRepository;
    private final com.manacommunity.api.marketplace.repository.MarketListingRepository marketListingRepository;
    private final com.manacommunity.api.jobs.repository.JobRepository jobRepository;

    @Override
    @Transactional
    public UserProfileResponse getProfile(AppUser user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    // Create default empty profile if none exists
                    UserProfile newProfile = UserProfile.builder()
                            .user(user)
                            .bio(null)
                            .skills(null)
                            .posts(0)
                            .connections(0)
                            .eventsAttended(0)
                            .itemsSold(0)
                            .jobsPosted(0)
                            .sportsPlayed(0)
                            .build();
                    return userProfileRepository.save(newProfile);
                });

        return mapToResponse(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(AppUser user, UserProfileRequest request) {
        // Update user fields
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (appUserRepository.existsByEmail(request.getEmail())) {
                throw new com.manacommunity.api.exception.DuplicateResourceException("User", "email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (appUserRepository.existsByPhone(request.getPhone())) {
                throw new com.manacommunity.api.exception.DuplicateResourceException("User", "phone", request.getPhone());
            }
            user.setPhone(request.getPhone());
        }
        if (request.getDob() != null) user.setDateOfBirth(request.getDob());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getFlatNo() != null) user.setFlatNo(request.getFlatNo());
        if (request.getBlock() != null) user.setBlock(request.getBlock());
        if (request.getProfilePicUrl() != null) user.setProfilePicUrl(request.getProfilePicUrl());
        appUserRepository.save(user);

        // Update profile fields
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().user(user).build());

        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getCoverPicUrl() != null) profile.setCoverPicUrl(request.getCoverPicUrl());
        if (request.getSkills() != null) {
            String skillsStr = request.getSkills().stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .collect(Collectors.joining(","));
            profile.setSkills(skillsStr);
        }

        userProfileRepository.save(profile);

        return mapToResponse(user, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse.UserStats getProfileStats(AppUser user) {
        // 1. Real posts created by the user
        long realPosts = 0;
        try {
            realPosts = postRepository.countByUserIdAndDeletedFalse(user.getId());
        } catch (Exception e) {
            // fallback
        }

        // 2. Real community network connections (Total residents in user's community)
        long realNetwork = 0;
        try {
            if (user.getCommunity() != null && user.getCommunity().getId() != null) {
                realNetwork = appUserRepository.countByCommunityId(user.getCommunity().getId());
            }
        } catch (Exception e) {
            // fallback
        }

        // 3. Real marketplace items listed by user
        long realItems = 0;
        try {
            realItems = marketListingRepository.countBySellerId(user.getId());
        } catch (Exception e) {
            // fallback
        }

        // 4. Real jobs posted by user
        long realJobs = 0;
        try {
            realJobs = jobRepository.countByPostedById(user.getId());
        } catch (Exception e) {
            // fallback
        }

        // 5. Real events attended and sports played from registrations
        int realEventsAttended = 0;
        int realSportsPlayed = 0;
        try {
            List<com.manacommunity.api.model.SportsEventRegistration> regs = 
                    sportsEventRegistrationRepository.findByUserId(user.getId());

            realEventsAttended = (int) regs.stream()
                    .filter(r -> r.getStatus() != com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.WITHDRAWN)
                    .count();

            realSportsPlayed = (int) regs.stream()
                    .filter(r -> r.getStatus() != com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.WITHDRAWN &&
                                 r.getEvent() != null && r.getEvent().getSport() != null && r.getEvent().getSport().getId() != null)
                    .map(r -> r.getEvent().getSport().getId())
                    .distinct()
                    .count();
        } catch (Exception e) {
            // fallback
        }

        return UserProfileResponse.UserStats.builder()
                .posts((int) realPosts)
                .connections((int) realNetwork)
                .eventsAttended(realEventsAttended)
                .itemsSold((int) realItems)
                .jobsPosted((int) realJobs)
                .sportsPlayed(realSportsPlayed)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse.UserActivityItem> getUserActivities(AppUser user) {
        List<ActivityCandidate> candidates = new ArrayList<>();

        // 1. User's Posts
        try {
            Page<com.manacommunity.api.model.Post> posts = postRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
                    user.getId(), PageRequest.of(0, 10));
            for (com.manacommunity.api.model.Post p : posts) {
                String title = p.getTitle();
                if (title == null || title.isBlank()) {
                    title = truncate(p.getContent(), 60);
                }
                candidates.add(new ActivityCandidate(
                        p.getId(),
                        "post",
                        "Posted in Community Feed: '" + title + "'",
                        p.getCreatedAt(),
                        "indigo",
                        "message"
                ));
            }
        } catch (Exception e) {
            // Silently fallback if table has issues
        }

        // 2. User's Marketplace Listings
        try {
            List<com.manacommunity.api.marketplace.entity.MarketListing> listings = 
                    marketListingRepository.findBySellerIdOrderByCreatedAtDesc(user.getId());
            for (com.manacommunity.api.marketplace.entity.MarketListing l : listings) {
                String priceStr = l.getPrice() != null ? " for ₹" + l.getPrice().toPlainString() : "";
                candidates.add(new ActivityCandidate(
                        l.getId(),
                        "marketplace",
                        "Listed '" + truncate(l.getTitle(), 45) + "' on Marketplace" + priceStr,
                        l.getCreatedAt(),
                        "emerald",
                        "package"
                ));
            }
        } catch (Exception e) {
            // Silently fallback
        }

        // 3. User's Sports & Event Registrations
        try {
            List<com.manacommunity.api.model.SportsEventRegistration> regs = 
                    sportsEventRegistrationRepository.findByUserId(user.getId());
            for (com.manacommunity.api.model.SportsEventRegistration r : regs) {
                if (r.getStatus() != com.manacommunity.api.model.SportsEventRegistration.RegistrationStatus.WITHDRAWN) {
                    String eventName = r.getEvent() != null && r.getEvent().getName() != null 
                            ? r.getEvent().getName() 
                            : (r.getEvent() != null && r.getEvent().getSport() != null ? r.getEvent().getSport().getName() + " Event" : "Sports Event");
                    candidates.add(new ActivityCandidate(
                            r.getId(),
                            "event",
                            "Registered for '" + eventName + "'",
                            r.getRegisteredAt() != null ? r.getRegisteredAt() : (r.getUpdatedAt() != null ? r.getUpdatedAt() : user.getCreatedAt()),
                            "yellow",
                            "trophy"
                    ));
                }
            }
        } catch (Exception e) {
            // Silently fallback
        }

        // 4. KYC Status Milestone
        if ("VERIFIED".equalsIgnoreCase(user.getKycStatus())) {
            candidates.add(new ActivityCandidate(
                    99901L,
                    "security",
                    "Completed Resident KYC verification successfully",
                    user.getCreatedAt() != null ? user.getCreatedAt().plusMinutes(5) : java.time.LocalDateTime.now(),
                    "emerald",
                    "shield-check"
            ));
        }

        // 5. User Joined Community Milestone
        String commName = user.getCommunity() != null && user.getCommunity().getName() != null 
                ? user.getCommunity().getName() 
                : "Mana Community";
        candidates.add(new ActivityCandidate(
                99900L,
                "community",
                "Joined " + commName + " community",
                user.getCreatedAt() != null ? user.getCreatedAt() : java.time.LocalDateTime.now().minusDays(30),
                "indigo",
                "users"
        ));

        // Sort descending by timestamp (null safe)
        candidates.sort((a, b) -> {
            if (a.timestamp == null && b.timestamp == null) return 0;
            if (a.timestamp == null) return 1;
            if (b.timestamp == null) return -1;
            return b.timestamp.compareTo(a.timestamp);
        });

        // Convert to DTO
        long seq = 1;
        List<UserProfileResponse.UserActivityItem> result = new ArrayList<>();
        for (ActivityCandidate c : candidates) {
            result.add(UserProfileResponse.UserActivityItem.builder()
                    .id(c.id != null ? c.id : seq++)
                    .type(c.type)
                    .text(c.text)
                    .time(formatTimeAgo(c.timestamp))
                    .timestamp(c.timestamp != null ? c.timestamp.toString() : "")
                    .iconType(c.iconType)
                    .color(c.color)
                    .build());
            if (result.size() >= 20) break;
        }

        return result;
    }

    private static class ActivityCandidate {
        Long id;
        String type;
        String text;
        java.time.LocalDateTime timestamp;
        String color;
        String iconType;

        ActivityCandidate(Long id, String type, String text, java.time.LocalDateTime timestamp, String color, String iconType) {
            this.id = id;
            this.type = type;
            this.text = text;
            this.timestamp = timestamp;
            this.color = color;
            this.iconType = iconType;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        String trimmed = s.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= max) return trimmed;
        return trimmed.substring(0, max - 3) + "...";
    }

    private String formatTimeAgo(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "Recently";
        java.time.Duration duration = java.time.Duration.between(dateTime, java.time.LocalDateTime.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) return "Just now";
        long minutes = duration.toMinutes();
        if (minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        long hours = duration.toHours();
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = duration.toDays();
        if (days == 1) return "Yesterday";
        if (days < 7) return days + " days ago";
        if (days < 30) {
            long weeks = days / 7;
            return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        }
        if (days < 365) {
            long months = days / 30;
            return months + (months == 1 ? " month ago" : " months ago");
        }
        long years = days / 365;
        return years + (years == 1 ? " year ago" : " years ago");
    }

    private UserProfileResponse mapToResponse(AppUser user, UserProfile profile) {
        List<String> skillsList = Collections.emptyList();
        if (profile.getSkills() != null && !profile.getSkills().trim().isEmpty()) {
            skillsList = Arrays.stream(profile.getSkills().split(","))
                    .map(String::trim)
                    .toList();
        }

        String joinedAtStr = user.getCreatedAt() != null 
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "";

        UserProfileResponse.UserStats stats = getProfileStats(user);

        return UserProfileResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dob(user.getDateOfBirth())
                .gender(user.getGender())
                .flatNo(user.getFlatNo())
                .block(user.getBlock())
                .role(user.getRole())
                .kycStatus(user.getKycStatus())
                .occupancyStatus(user.getOccupancyStatus())
                .residentType(user.getResidentType())
                .userType(user.getOccupancyStatus() != null ? user.getOccupancyStatus() : "Owner")
                .communityName(user.getCommunity() != null ? user.getCommunity().getName() : "")
                .communityType(user.getCommunity() != null ? user.getCommunity().getType() : "")
                .communityCode(user.getCommunity() != null ? user.getCommunity().getInviteCode() : "")
                .joinedAt(joinedAtStr)
                .bio(profile.getBio())
                .profilePicUrl(user.getProfilePicUrl())
                .coverPicUrl(profile.getCoverPicUrl())
                .skills(skillsList)
                .stats(stats)
                .build();
    }
}
