package com.manacommunity.api.sports.service.impl;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.SportsMatchStreamRepository;
import com.manacommunity.api.sports.service.SportsStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SportsStreamServiceImpl implements SportsStreamService {

    private final SportsMatchStreamRepository streamRepo;

    // ── Patterns for embed URL resolution ─────────────────────────
    private static final Pattern YT_WATCH  = Pattern.compile("youtube\\.com/watch\\?v=([A-Za-z0-9_-]{11})");
    private static final Pattern YT_SHORT  = Pattern.compile("youtu\\.be/([A-Za-z0-9_-]{11})");
    private static final Pattern YT_LIVE   = Pattern.compile("youtube\\.com/live/([A-Za-z0-9_-]{11})");
    private static final Pattern YT_EMBED  = Pattern.compile("youtube\\.com/embed/([A-Za-z0-9_-]{11})");
    private static final Pattern TWITCH    = Pattern.compile("twitch\\.tv/([A-Za-z0-9_]{4,25})");

    @Override
    @Transactional(readOnly = true)
    public SportsStreamResponse getStream(Long matchId) {
        SportsMatchStream stream = streamRepo.findByMatchId(matchId)
                .orElse(SportsMatchStream.builder().matchId(matchId).build());
        return toResponse(stream);
    }

    @Override
    @Transactional
    public SportsStreamResponse startStream(Long matchId, String title) {
        SportsMatchStream stream = streamRepo.findByMatchId(matchId)
                .orElseGet(() -> SportsMatchStream.builder().matchId(matchId).build());
        stream.setStatus(StreamStatus.LIVE);
        stream.setStartedAt(LocalDateTime.now());
        stream.setEndedAt(null);
        if (title != null && !title.isBlank()) stream.setTitle(title.trim());
        return toResponse(streamRepo.save(stream));
    }

    @Override
    @Transactional
    public SportsStreamResponse endStream(Long matchId) {
        SportsMatchStream stream = streamRepo.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Stream not found for match " + matchId));
        stream.setStatus(StreamStatus.ENDED);
        stream.setEndedAt(LocalDateTime.now());
        return toResponse(streamRepo.save(stream));
    }

    @Override
    @Transactional
    public SportsStreamResponse addCamera(Long matchId, SportsStreamCameraRequest req) {
        SportsMatchStream stream = streamRepo.findByMatchId(matchId)
                .orElseGet(() -> SportsMatchStream.builder().matchId(matchId).build());
        stream = streamRepo.save(stream); // ensure persisted before adding children

        EmbedResult embed = resolveEmbed(req.getUrl());
        boolean isFirst = stream.getCameras().isEmpty();
        SportsMatchStreamCamera cam = SportsMatchStreamCamera.builder()
                .stream(stream)
                .label(req.getLabel().trim())
                .url(req.getUrl().trim())
                .embedUrl(embed.embedUrl)
                .platform(embed.platform)
                .active(isFirst)
                .build();
        stream.getCameras().add(cam);
        return toResponse(streamRepo.save(stream));
    }

    @Override
    @Transactional
    public SportsStreamResponse removeCamera(Long matchId, Long cameraId) {
        SportsMatchStream stream = streamRepo.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Stream not found for match " + matchId));
        boolean wasActive = stream.getCameras().stream()
                .anyMatch(c -> c.getId().equals(cameraId) && Boolean.TRUE.equals(c.getActive()));
        stream.getCameras().removeIf(c -> c.getId().equals(cameraId));
        if (wasActive && !stream.getCameras().isEmpty()) {
            stream.getCameras().get(0).setActive(true);
        }
        return toResponse(streamRepo.save(stream));
    }

    @Override
    @Transactional
    public SportsStreamResponse switchActiveCamera(Long matchId, Long cameraId) {
        SportsMatchStream stream = streamRepo.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Stream not found for match " + matchId));
        stream.getCameras().forEach(c -> c.setActive(c.getId().equals(cameraId)));
        return toResponse(streamRepo.save(stream));
    }

    @Override
    @Transactional
    public SportsStreamResponse postMessage(Long matchId, SportsStreamChatRequest req, String authorName) {
        SportsMatchStream stream = streamRepo.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Stream not found for match " + matchId));
        SportsMatchStreamMessage msg = SportsMatchStreamMessage.builder()
                .stream(stream)
                .authorName(authorName)
                .text(req.getText().trim())
                .commentary(req.isCommentary())
                .build();
        stream.getMessages().add(msg);
        return toResponse(streamRepo.save(stream));
    }

    // ── Mapping ───────────────────────────────────────────────────

    private SportsStreamResponse toResponse(SportsMatchStream s) {
        return SportsStreamResponse.builder()
                .matchId(s.getMatchId())
                .status(s.getStatus())
                .title(s.getTitle())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .cameras(s.getCameras().stream().map(c -> SportsStreamResponse.CameraDto.builder()
                        .id(c.getId())
                        .label(c.getLabel())
                        .url(c.getUrl())
                        .embedUrl(c.getEmbedUrl())
                        .platform(c.getPlatform().name())
                        .active(Boolean.TRUE.equals(c.getActive()))
                        .build()).toList())
                .messages(s.getMessages().stream().map(m -> SportsStreamResponse.MessageDto.builder()
                        .id(m.getId())
                        .authorName(m.getAuthorName())
                        .text(m.getText())
                        .commentary(Boolean.TRUE.equals(m.getCommentary()))
                        .postedAt(m.getPostedAt())
                        .build()).toList())
                .build();
    }

    // ── Embed URL resolution ──────────────────────────────────────

    private record EmbedResult(String embedUrl, StreamPlatform platform) {}

    private EmbedResult resolveEmbed(String raw) {
        String url = raw.trim();

        for (Pattern p : new Pattern[]{YT_WATCH, YT_SHORT, YT_LIVE, YT_EMBED}) {
            Matcher m = p.matcher(url);
            if (m.find()) {
                String vid = m.group(1);
                return new EmbedResult(
                        "https://www.youtube.com/embed/" + vid + "?autoplay=1&rel=0&modestbranding=1",
                        StreamPlatform.YOUTUBE);
            }
        }

        Matcher tw = TWITCH.matcher(url);
        if (tw.find()) {
            return new EmbedResult(
                    "https://player.twitch.tv/?channel=" + tw.group(1) + "&parent=mana.community",
                    StreamPlatform.TWITCH);
        }

        return new EmbedResult(url, StreamPlatform.CUSTOM);
    }
}
