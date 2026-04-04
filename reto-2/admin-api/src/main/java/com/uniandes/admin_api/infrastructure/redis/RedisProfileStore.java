package com.uniandes.admin_api.infrastructure.redis;

import com.uniandes.admin_api.domain.model.AdminProfileDTO;
import com.uniandes.admin_api.domain.model.AdminProfileStatus;
import com.uniandes.admin_api.domain.model.AdminTrustedContextDTO;
import com.uniandes.admin_api.domain.model.ContextType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class RedisProfileStore {

    private static final String PROFILE_PREFIX = "profile:";
    private static final String STATUS_SUFFIX = ":status";
    private static final String DEVICES_SUFFIX = ":devices";
    private static final String IPS_SUFFIX = ":ips";
    private static final String HOURS_SUFFIX = ":hours";
    private static final String AGENTS_SUFFIX = ":agents";
    private static final String REGIONS_SUFFIX = ":regions";

    private final StringRedisTemplate redisTemplate;

    public RedisProfileStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveProfile(AdminProfileDTO profile) {
        String adminId = profile.getAdminId();
        String keyPrefix = PROFILE_PREFIX + adminId;

        // Save status
        redisTemplate.opsForValue().set(keyPrefix + STATUS_SUFFIX, profile.getStatus().name());

        // Clear existing sets before populating
        clearProfileSets(adminId);

        // Populate trusted contexts by type
        if (profile.getTrustedContexts() != null) {
            for (AdminTrustedContextDTO context : profile.getTrustedContexts()) {
                String setKey = getSetKeyForContextType(adminId, context.getType());
                if (setKey != null) {
                    redisTemplate.opsForSet().add(setKey, context.getValue());
                }
            }
        }

        log.info("Saved profile to Redis for adminId: {}", adminId);
    }

    public Optional<AdminProfileStatus> getStatus(String adminId) {
        String key = PROFILE_PREFIX + adminId + STATUS_SUFFIX;
        String status = redisTemplate.opsForValue().get(key);
        if (status == null) {
            return Optional.empty();
        }
        return Optional.of(AdminProfileStatus.valueOf(status));
    }

    public Set<String> getDevices(String adminId) {
        return redisTemplate.opsForSet().members(PROFILE_PREFIX + adminId + DEVICES_SUFFIX);
    }

    public Set<String> getIps(String adminId) {
        return redisTemplate.opsForSet().members(PROFILE_PREFIX + adminId + IPS_SUFFIX);
    }

    public Set<String> getHours(String adminId) {
        return redisTemplate.opsForSet().members(PROFILE_PREFIX + adminId + HOURS_SUFFIX);
    }

    public Set<String> getAgents(String adminId) {
        return redisTemplate.opsForSet().members(PROFILE_PREFIX + adminId + AGENTS_SUFFIX);
    }

    public Set<String> getRegions(String adminId) {
        return redisTemplate.opsForSet().members(PROFILE_PREFIX + adminId + REGIONS_SUFFIX);
    }

    public boolean profileExists(String adminId) {
        String key = PROFILE_PREFIX + adminId + STATUS_SUFFIX;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void deleteProfile(String adminId) {
        String keyPrefix = PROFILE_PREFIX + adminId;
        redisTemplate.delete(keyPrefix + STATUS_SUFFIX);
        clearProfileSets(adminId);
        log.info("Deleted profile from Redis for adminId: {}", adminId);
    }

    private void clearProfileSets(String adminId) {
        String keyPrefix = PROFILE_PREFIX + adminId;
        redisTemplate.delete(keyPrefix + DEVICES_SUFFIX);
        redisTemplate.delete(keyPrefix + IPS_SUFFIX);
        redisTemplate.delete(keyPrefix + HOURS_SUFFIX);
        redisTemplate.delete(keyPrefix + AGENTS_SUFFIX);
        redisTemplate.delete(keyPrefix + REGIONS_SUFFIX);
    }

    private String getSetKeyForContextType(String adminId, ContextType type) {
        String keyPrefix = PROFILE_PREFIX + adminId;
        return switch (type) {
            case DEVICE -> keyPrefix + DEVICES_SUFFIX;
            case IP -> keyPrefix + IPS_SUFFIX;
            case HOUR -> keyPrefix + HOURS_SUFFIX;
            case USER_AGENT -> keyPrefix + AGENTS_SUFFIX;
            case REGION -> keyPrefix + REGIONS_SUFFIX;
        };
    }
}
