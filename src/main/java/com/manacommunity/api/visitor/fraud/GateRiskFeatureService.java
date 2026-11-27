package com.manacommunity.api.visitor.fraud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GateRiskFeatureService {

    private final StringRedisTemplate redisTemplate;

    public GateRiskFeatureService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public GateRiskFeatureVector extractFeatures(String phoneNumber, Long communityId, String flatNumber, String vehicleNumber) {
        long start = System.currentTimeMillis();
        int currentHour = LocalTime.now().getHour();

        if (redisTemplate == null) {
            log.debug("Redis unavailable, returning safe degraded feature vector");
            return GateRiskFeatureVector.safeDegraded(phoneNumber, communityId, flatNumber, currentHour);
        }

        try {
            long now = System.currentTimeMillis();
            long oneHourAgo = now - 3_600_000L;
            long oneDayAgo = now - 86_400_000L;
            String phoneHash = sha256Hex(phoneNumber != null ? phoneNumber.trim() : "UNKNOWN");

            String velocityPhoneKey = "velocity:phone:" + phoneHash + ":societies";
            String velocityFlatKey = "velocity:flat:" + communityId + ":" + (flatNumber != null ? flatNumber.trim() : "GENERAL") + ":entries";
            String flatBaselineKey = "flat:" + communityId + ":" + (flatNumber != null ? flatNumber.trim() : "GENERAL") + ":baseline";
            String blacklistPhoneKey = "consortium:blacklist:phones";
            String deliveryPartnerKey = "delivery:active_partner:" + phoneHash;

            // Pipelined execution across all 5 keys in 1 round trip (< 3ms)
            List<Object> results = redisTemplate.executePipelined(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    byte[] velPhoneBytes = velocityPhoneKey.getBytes(StandardCharsets.UTF_8);
                    byte[] velFlatBytes = velocityFlatKey.getBytes(StandardCharsets.UTF_8);
                    byte[] flatBaseBytes = flatBaselineKey.getBytes(StandardCharsets.UTF_8);
                    byte[] blackBytes = blacklistPhoneKey.getBytes(StandardCharsets.UTF_8);
                    byte[] partnerBytes = deliveryPartnerKey.getBytes(StandardCharsets.UTF_8);
                    byte[] phoneValBytes = phoneHash.getBytes(StandardCharsets.UTF_8);
                    byte[] entryMember = (communityId + ":" + now).getBytes(StandardCharsets.UTF_8);

                    // 1. Sliding window: prune past 1h and add current entry
                    connection.zSetCommands().zRemRangeByScore(velPhoneBytes, 0, oneHourAgo);
                    connection.zSetCommands().zAdd(velPhoneBytes, now, entryMember);
                    connection.zSetCommands().zCard(velPhoneBytes);
                    connection.keyCommands().expire(velPhoneBytes, 7200L); // 2h TTL

                    // 2. Sliding window flat entries (past 24h)
                    connection.zSetCommands().zRemRangeByScore(velFlatBytes, 0, oneDayAgo);
                    connection.zSetCommands().zAdd(velFlatBytes, now, entryMember);
                    connection.zSetCommands().zCard(velFlatBytes);
                    connection.keyCommands().expire(velFlatBytes, 86400L);

                    // 3. Flat historical baseline profile
                    connection.hashCommands().hGetAll(flatBaseBytes);

                    // 4. Consortium blacklist check
                    connection.setCommands().sIsMember(blackBytes, phoneValBytes);

                    // 5. Active delivery partner check
                    connection.keyCommands().exists(partnerBytes);

                    return null;
                }
            });

            long extractionLatency = System.currentTimeMillis() - start;

            // Parse pipelined results
            long societies1h = results != null && results.size() > 2 && results.get(2) instanceof Number num ? num.longValue() : 1L;
            long flatEntries24h = results != null && results.size() > 6 && results.get(6) instanceof Number num ? num.longValue() : 1L;

            double historicalNightRatio = 0.05;
            boolean isVacationMode = false;
            if (results != null && results.size() > 8 && results.get(8) instanceof Map<?, ?> map) {
                Object nightObj = map.get("hist_night_pct".getBytes(StandardCharsets.UTF_8));
                if (nightObj != null) {
                    try { historicalNightRatio = Double.parseDouble(new String((byte[]) nightObj, StandardCharsets.UTF_8)); } catch (Exception ignored) {}
                }
                Object vacObj = map.get("vacation_mode".getBytes(StandardCharsets.UTF_8));
                if (vacObj != null) {
                    isVacationMode = "1".equals(new String((byte[]) vacObj, StandardCharsets.UTF_8));
                }
            }

            boolean isBlacklisted = results != null && results.size() > 9 && Boolean.TRUE.equals(results.get(9));
            boolean isDeliveryPartner = results != null && results.size() > 10 && Boolean.TRUE.equals(results.get(10));

            return GateRiskFeatureVector.builder()
                    .phoneNumber(phoneNumber)
                    .phoneHash(phoneHash)
                    .vehicleNumber(vehicleNumber)
                    .communityId(communityId)
                    .flatNumber(flatNumber)
                    .societiesVisitedPastHour(societies1h)
                    .entriesSameFlatPast24Hours(flatEntries24h)
                    .currentHour(currentHour)
                    .isNightEntry(currentHour >= 0 && currentHour < 6)
                    .flatHistoricalNightVisitRatio(historicalNightRatio)
                    .isResidentVacationMode(isVacationMode)
                    .isConsortiumBlacklisted(isBlacklisted)
                    .isVerifiedDeliveryPartner(isDeliveryPartner)
                    .extractionLatencyMs(extractionLatency)
                    .isDegraded(false)
                    .build();

        } catch (Exception e) {
            log.warn("Redis feature extraction failed after {}ms: {}", (System.currentTimeMillis() - start), e.getMessage());
            return GateRiskFeatureVector.safeDegraded(phoneNumber, communityId, flatNumber, currentHour);
        }
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 16); // 16-char compact hash
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
