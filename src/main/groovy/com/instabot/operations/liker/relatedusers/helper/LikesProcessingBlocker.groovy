package com.instabot.operations.liker.relatedusers.helper

import com.instabot.config.InstaBotConfig
import com.instabot.data.model.primaryuser.PrimaryUser
import com.instabot.data.services.interaction.like.LikeInteractionDataService
import com.instabot.data.services.primaryuser.PrimaryUserDataService
import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

import java.time.LocalDateTime

// TODO Document
@Component
class LikesProcessingBlocker {
    private static final Logger LOG = LogManager.getLogger(LikesProcessingBlocker.class)

    @Autowired
    private InstaBotConfig instaBotConfig
    @Autowired
    private LikeInteractionDataService likeInteractionDataService
    @Autowired
    private PrimaryUserDataService primaryUserDataService
    @Autowired
    private InstaWebDriver instaDriver

    private int maxLikesPerHour
    private int maxLikesPer24Hours

    @Bean("initializeLikesProcessingBlocker")
    private void initialize() {
        LOG.info("Initialize LikesProcessingBlocker")

        maxLikesPerHour = instaBotConfig.getIniFile().get("user-liker", "max-likes-per-hour", Integer.class)
        LOG.info("Max. posts to be liked per hour: $maxLikesPerHour")

        maxLikesPer24Hours = instaBotConfig.getIniFile().get("user-liker", "max-likes-per-day", Integer.class)
        LOG.info("Max. posts to be liked per hour: $maxLikesPer24Hours")

        // TODO reset the block
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
            LOG.debug("Likes processing is blocked until: " + primaryUser.likesProcessingBlockedUntil) // TODO Format LDT
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

    // TODO Format LDT
    private boolean hasReachedHourlyLimit(PrimaryUser primaryUser, int likesLastHour) {
        if (likesLastHour >= maxLikesPerHour) {
            LocalDateTime blockLikesProcessingUntil = LocalDateTime.now().plusHours(1)
            LOG.info("Primary-user $primaryUser.username has reached hourly likes limit; nr. of likes: $likesLastHour; block likes processing until: $blockLikesProcessingUntil")
            primaryUserDataService.save(primaryUser.setLikesProcessingBlockedUntil(blockLikesProcessingUntil))
            return true
        }
        return false
    }

    private boolean hasReachedDailyLimit(PrimaryUser primaryUser, int likesLast24Hours) {
        if (likesLast24Hours >= maxLikesPer24Hours) {
            LocalDateTime blockLikesProcessingUntil = LocalDateTime.now().plusDays(1)
            LOG.info("Primary-user $primaryUser.username has reached daily likes limit; nr. of likes: $likesLast24Hours; block likes processing until: $blockLikesProcessingUntil")
            primaryUserDataService.save(primaryUser.setLikesProcessingBlockedUntil(blockLikesProcessingUntil))
            return true
        }
        return false
    }

}
