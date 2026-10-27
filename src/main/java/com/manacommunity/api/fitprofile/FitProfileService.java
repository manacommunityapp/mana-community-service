package com.manacommunity.api.fitprofile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FitProfileService {

    public record Profile(String faceShape, BigDecimal widthRatio, BigDecimal neckRatio, BigDecimal confidence) {}

    private final JdbcTemplate jdbc;

    @Transactional
    public void save(Long userId, Profile p) {
        jdbc.update("""
                INSERT INTO fit_profiles (user_id, face_shape, width_ratio, neck_ratio, confidence)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (user_id) DO UPDATE SET
                    face_shape = EXCLUDED.face_shape,
                    width_ratio = EXCLUDED.width_ratio,
                    neck_ratio = EXCLUDED.neck_ratio,
                    confidence = EXCLUDED.confidence,
                    updated_at = now()
                """,
                userId, p.faceShape(), p.widthRatio(), p.neckRatio(), p.confidence());
        log.info("Saved fit profile for userId={}, shape={}", userId, p.faceShape());
    }

    @Transactional
    public void delete(Long userId) {
        jdbc.update("DELETE FROM fit_profiles WHERE user_id = ?", userId);
        log.info("Deleted fit profile for userId={}", userId);
    }

    @Transactional(readOnly = true)
    public Optional<String> getFaceShape(Long userId) {
        String shape = jdbc.query(
                "SELECT face_shape FROM fit_profiles WHERE user_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, userId);
        return Optional.ofNullable(shape);
    }

    @Transactional(readOnly = true)
    public Optional<Profile> getProfile(Long userId) {
        return jdbc.query(
                "SELECT face_shape, width_ratio, neck_ratio, confidence FROM fit_profiles WHERE user_id = ?",
                rs -> {
                    if (rs.next()) {
                        return Optional.of(new Profile(
                                rs.getString("face_shape"),
                                rs.getBigDecimal("width_ratio"),
                                rs.getBigDecimal("neck_ratio"),
                                rs.getBigDecimal("confidence")
                        ));
                    }
                    return Optional.empty();
                }, userId);
    }
}
