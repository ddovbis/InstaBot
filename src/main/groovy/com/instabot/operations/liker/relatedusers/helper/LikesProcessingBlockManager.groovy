package com.instabot.operations.liker.relatedusers.helper

import com.instabot.config.InstaBotConfig
import com.instabot.data.model.primaryuser.PrimaryUser
import com.instabot.data.services.interaction.like.LikeInteractionDataService
import com.instabot.data.services.primaryuser.PrimaryUserDataService
import com.instabot.utils.time.TimeUtils
import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

import java.time.LocalDateTime

// TODO Document
@Component
class LikesProcessingBlockManager {
    private static final Logger LOG = LogManager.getLogger(LikesProcessingBlockManager.class)

    @Autowired
    private InstaBotConfig instaBotConfig
    @Autowired
    private LikeInteractionDataService likeInteractionDataService
    @Autowired
    private PrimaryUserDataService primaryUserDataService
    @Autowired
    private InstaWebDriver instaDriver

    private boolean forceUnblockPrimaryUserLikesProcessing
    private int maxLikesPerHour
    private int maxLikesPer24Hours

    @Bean("initializeLikesProcessingBlocker")
    @DependsOn("initializeInstaWebDriver")
    private void initialize() {
        LOG.info("Initialize LikesProcessingBlockManager")

        forceUnblockPrimaryUserLikesProcessing = instaBotConfig.getIniFile().get("user-liker", "force-unblock-primary-user-likes-processing", Boolean.class)
        LOG.info("Force unblock primary user likes processing: $forceUnblockPrimaryUserLikesProcessing")
        unblockPrimaryUserLikesProcessingIfNecessary()

        maxLikesPerHour = instaBotConfig.getIniFile().get("user-liker", "max-likes-per-hour", Integer.class)
        LOG.info("Max. posts to be liked per hour: $maxLikesPerHour")

        maxLikesPer24Hours = instaBotConfig.getIniFile().get("user-liker", "max-likes-per-day", Integer.class)
        LOG.info("Max. posts to be liked per 24 hours: $maxLikesPer24Hours")
    }

    void unblockPrimaryUserLikesProcessingIfNecessary() {
        LOG.info("Unblock primary user likes processing if necessary")
        PrimaryUser primaryUser = instaDriver.getPrimaryUser()
        LocalDateTime likesProcessingBlockedUntil = primaryUser.likesProcessingBlockedUntil

        if (likesProcessingBlockedUntil == null) {
            LOG.info("Likes processing has not been blocked for the user; no action is necessary")
            return
        } else if (likesProcessingBlockedUntil < LocalDateTime.now()) {
            LOG.info("Likes processing block is past due, the following value will be reverted to 'null': ${TimeUtils.getLegibleDateTime(likesProcessingBlockedUntil)}")
            saveNewLikesProcessingBlockedUntilValue(primaryUser, null)
            return
        } else if (forceUnblockPrimaryUserLikesProcessing) {
            LOG.info("'Force unblock primary user likes processing' is enabled,the following value will be reverted to 'null': ${TimeUtils.getLegibleDateTime(likesProcessingBlockedUntil)}")
            return
        }

        LOG.info("Unblocking primary user likes processing requirements are not met")
    }

    boolean blockPrimaryUserLikesProcessingIfNecessary() {
        PrimaryUser primaryUser = instaDriver.getPrimaryUser()

        if (isLikesProcessingBlocked(primaryUser)) {
            return true
        }

        int likesLastHour = countLikesLastHour()
        int likesLast24Hours = countLikesLast24Hours()
        return (hasReachedHourlyLimit(primaryUser, likesLastHour) || hasReachedDailyLimit(primaryUser, likesLast24Hours))
    }

    private boolean isLikesProcessingBlocked(PrimaryUser primaryUser) {
        if (primaryUser.likesProcessingBlockedUntil > LocalDateTime.now()) {
            LOG.debug("Likes processing is blocked until: " + TimeUtils.getLegibleDateTime(primaryUser.likesProcessingBlockedUntil))
            return true
        }
        return false
    }

    private int countLikesLastHour() {
        return likeInteractionDataService.countByPrimaryUsernameBetween(instaDriver.primaryUsername, LocalDateTime.now().minusHours(1), LocalDateTime.now())
    }

    private int countLikesLast24Hours() {
        return likeInteractionDataService.countByPrimaryUsernameBetween(instaDriver.primaryUsername, LocalDateTime.now().minusHours(24), LocalDateTime.now())
    }

    private boolean hasReachedHourlyLimit(PrimaryUser primaryUser, int likesLastHour) {
        if (likesLastHour >= maxLikesPerHour) {
            LocalDateTime blockLikesProcessingUntil = LocalDateTime.now().plusHours(1)
            LOG.info("Primary-user $primaryUser.username has reached hourly likes limit; " +
                    "nr. of likes: $likesLastHour; " +
                    "block likes processing until: ${TimeUtils.getLegibleDateTime(blockLikesProcessingUntil)}")
            saveNewLikesProcessingBlockedUntilValue(primaryUser, blockLikesProcessingUntil)
            return true
        }
        return false
    }

    private boolean hasReachedDailyLimit(PrimaryUser primaryUser, int likesLast24Hours) {
        if (likesLast24Hours >= maxLikesPer24Hours) {
            LocalDateTime blockLikesProcessingUntil = LocalDateTime.now().plusDays(1)
            LOG.info("Primary-user $primaryUser.username has reached daily likes limit; " +
                    "nr. of likes: $likesLast24Hours; " +
                    "block likes processing until: ${TimeUtils.getLegibleDateTime(blockLikesProcessingUntil)}")
            saveNewLikesProcessingBlockedUntilValue(primaryUser, blockLikesProcessingUntil)
            return true
        }
        return false
    }

    private void saveNewLikesProcessingBlockedUntilValue(PrimaryUser primaryUser, LocalDateTime blockLikesProcessingUntil) {
        primaryUserDataService.save(primaryUser.setLikesProcessingBlockedUntil(blockLikesProcessingUntil))
    }
}
