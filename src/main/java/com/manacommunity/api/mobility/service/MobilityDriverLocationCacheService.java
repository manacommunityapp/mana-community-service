package com.manacommunity.api.mobility.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-backed driver GPS location cache and geospatial index.
 *
 * <p>Avoids writing to PostgreSQL on every driver location ping:
 * <ul>
 *   <li>Writes coordinates to {@code mobility:driver:{id}:location} (HASH, 2-min TTL).</li>
 *   <li>Maintains {@code mobility:drivers:geo} (GEO ZSET) for fast nearest-driver dispatch.</li>
 *   <li>Tracks dirty coordinates for periodic 30s batch persistence via
 *       {@link MobilityDriverLocationSyncScheduler}.</li>
 * </ul>
 * Falls back gracefully (returns {@code false} / empty) when Redis is unavailable so
 * {@code MobilityServiceImpl} writes directly to the DB and uses distance calculation.</p>
 */
@Slf4j
@Service
public class MobilityDriverLocationCacheService {

    public static final String GEO_KEY        = "mobility:drivers:geo";
    private static final String LOC_PREFIX    = "mobility:driver:";
    private static final String LOC_SUFFIX    = ":location";
    private static final Duration LOC_TTL     = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<Long, CachedDriverLocation> dirtyLocations = new ConcurrentHashMap<>();

    public MobilityDriverLocationCacheService(
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public record CachedDriverLocation(double lat, double lng, Instant updatedAt) {}

    /**
     * Caches a driver's latest GPS coordinates in Redis and updates the GEO index.
     *
     * @return {@code true} if cached in Redis (caller can skip immediate DB write),
     *         {@code false} if Redis is unavailable/failed (caller should persist to DB).
     */
    public boolean updateLocation(Long driverId, double lat, double lng, boolean isOnline) {
        if (redisTemplate == null || driverId == null) {
            return false;
        }
        try {
            Instant now = Instant.now();
            String hashKey = LOC_PREFIX + driverId + LOC_SUFFIX;
            HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
            hashOps.putAll(hashKey, Map.of(
                    "lat", Double.toString(lat),
                    "lng", Double.toString(lng),
                    "updatedAt", Long.toString(now.toEpochMilli())
            ));
            redisTemplate.expire(hashKey, LOC_TTL);

            String member = String.valueOf(driverId);
            if (isOnline) {
                // Note: Spring Data Redis Point uses (x=longitude, y=latitude)
                redisTemplate.opsForGeo().add(GEO_KEY, new Point(lng, lat), member);
            } else {
                redisTemplate.opsForGeo().remove(GEO_KEY, member);
            }

            dirtyLocations.put(driverId, new CachedDriverLocation(lat, lng, now));
            return true;
        } catch (Exception e) {
            log.warn("Redis driver location update failed for driverId={}: {}", driverId, e.getMessage());
            return false;
        }
    }

    /**
     * Updates the GEO index when a driver's availability changes (e.g. ONLINE vs OFFLINE/OFFER_PENDING).
     */
    public void updateAvailabilityIndex(Long driverId, boolean isOnline, Double lat, Double lng) {
        if (redisTemplate == null || driverId == null) return;
        try {
            String member = String.valueOf(driverId);
            if (isOnline && lat != null && lng != null) {
                redisTemplate.opsForGeo().add(GEO_KEY, new Point(lng, lat), member);
            } else if (!isOnline) {
                redisTemplate.opsForGeo().remove(GEO_KEY, member);
            }
        } catch (Exception e) {
            log.debug("Redis GEO availability update skipped for driverId={}: {}", driverId, e.getMessage());
        }
    }

    /**
     * Reads the freshest cached location for a driver from Redis.
     */
    public Optional<CachedDriverLocation> getLocation(Long driverId) {
        if (redisTemplate == null || driverId == null) {
            return Optional.empty();
        }
        try {
            String hashKey = LOC_PREFIX + driverId + LOC_SUFFIX;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(hashKey);
            if (entries == null || entries.isEmpty()) {
                return Optional.empty();
            }
            Object rawLat = entries.get("lat");
            Object rawLng = entries.get("lng");
            Object rawTs  = entries.get("updatedAt");
            if (rawLat == null || rawLng == null) {
                return Optional.empty();
            }
            double lat = Double.parseDouble(rawLat.toString());
            double lng = Double.parseDouble(rawLng.toString());
            Instant updatedAt = rawTs != null
                    ? Instant.ofEpochMilli(Long.parseLong(rawTs.toString()))
                    : Instant.now();
            return Optional.of(new CachedDriverLocation(lat, lng, updatedAt));
        } catch (Exception e) {
            log.debug("Redis driver location read failed for driverId={}: {}", driverId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Queries Redis GEO index for nearest drivers within {@code radiusKm}, ordered closest-first.
     */
    public List<Long> findNearestDriverIds(double pickupLat, double pickupLng, double radiusKm, int limit) {
        if (redisTemplate == null) {
            return Collections.emptyList();
        }
        try {
            Circle area = new Circle(new Point(pickupLng, pickupLat), new Distance(radiusKm, Metrics.KILOMETERS));
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .sortAscending()
                    .limit(limit);
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().radius(GEO_KEY, area, args);
            if (results == null) {
                return Collections.emptyList();
            }
            return results.getContent().stream()
                    .map(r -> r.getContent() != null ? r.getContent().getName() : null)
                    .filter(name -> name != null && !name.isBlank())
                    .map(Long::valueOf)
                    .toList();
        } catch (Exception e) {
            log.debug("Redis GEO search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Atomically drains dirty locations accumulated since the last DB sync.
     */
    public Map<Long, CachedDriverLocation> drainDirtyLocations() {
        if (dirtyLocations.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, CachedDriverLocation> snapshot = new HashMap<>();
        for (Long id : dirtyLocations.keySet()) {
            CachedDriverLocation loc = dirtyLocations.remove(id);
            if (loc != null) {
                snapshot.put(id, loc);
            }
        }
        return snapshot;
    }
}
