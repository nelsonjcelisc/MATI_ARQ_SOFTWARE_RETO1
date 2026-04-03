package com.uniandes.admin_api.infrastructure.redis;

import com.uniandes.admin_api.domain.model.AdminProfileDTO;
import com.uniandes.admin_api.domain.model.AdminProfileStatus;
import com.uniandes.admin_api.domain.repository.AdminProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ProfileCacheLoader {

    private final AdminProfileRepository adminProfileRepository;
    private final RedisProfileStore redisProfileStore;

    public ProfileCacheLoader(AdminProfileRepository adminProfileRepository, RedisProfileStore redisProfileStore) {
        this.adminProfileRepository = adminProfileRepository;
        this.redisProfileStore = redisProfileStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadProfilesToRedis() {
        log.info("Starting to load admin profiles into Redis cache...");

        try {
            // Load ACTIVE profiles
            List<AdminProfileDTO> activeProfiles = adminProfileRepository.findByStatus(AdminProfileStatus.ACTIVE);
            int activeCount = loadProfiles(activeProfiles);
            log.info("Loaded {} ACTIVE profiles into Redis", activeCount);

            // Load LEARNING profiles (they also need to be in Redis for scoring)
            List<AdminProfileDTO> learningProfiles = adminProfileRepository.findByStatus(AdminProfileStatus.LEARNING);
            int learningCount = loadProfiles(learningProfiles);
            log.info("Loaded {} LEARNING profiles into Redis", learningCount);

            // Load BLOCKED profiles (to reject requests immediately)
            List<AdminProfileDTO> blockedProfiles = adminProfileRepository.findByStatus(AdminProfileStatus.BLOCKED);
            int blockedCount = loadProfiles(blockedProfiles);
            log.info("Loaded {} BLOCKED profiles into Redis", blockedCount);

            log.info("Finished loading {} total profiles into Redis cache",
                    activeCount + learningCount + blockedCount);

        } catch (Exception e) {
            log.error("Failed to load profiles into Redis cache", e);
        }
    }

    private int loadProfiles(List<AdminProfileDTO> profiles) {
        int count = 0;
        for (AdminProfileDTO profile : profiles) {
            try {
                redisProfileStore.saveProfile(profile);
                count++;
            } catch (Exception e) {
                log.error("Failed to load profile for adminId: {}", profile.getAdminId(), e);
            }
        }
        return count;
    }

    public void reloadAllProfiles() {
        log.info("Reloading all admin profiles into Redis cache...");
        loadProfilesToRedis();
    }

    public void reloadProfile(String adminId) {
        log.info("Reloading profile for adminId: {}", adminId);
        adminProfileRepository.findByAdminId(adminId)
                .ifPresentOrElse(
                        redisProfileStore::saveProfile,
                        () -> {
                            redisProfileStore.deleteProfile(adminId);
                            log.warn("Profile not found in DB for adminId: {}, removed from Redis", adminId);
                        }
                );
    }
}
