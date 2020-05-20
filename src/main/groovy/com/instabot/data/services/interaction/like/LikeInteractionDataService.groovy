package com.instabot.data.services.interaction.like


import com.instabot.data.model.interaction.like.LikeInteraction
import com.instabot.data.model.primaryuser.PrimaryUser
import com.instabot.data.repositories.interaction.like.LikeInteractionRepository
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.time.LocalDateTime

@Service
class LikeInteractionDataService {
    private static final Logger LOG = LogManager.getLogger(LikeInteractionDataService.class)

    @Autowired
    private LikeInteractionRepository likeInteractionRepository

    void save(LikeInteraction likeInteraction) {
        if (likeInteraction == null) {
            throw new NullPointerException("Attempted to save null like interaction to database")
        }
        LOG.debug("Save like interaction: $likeInteraction")
        likeInteractionRepository.save(likeInteraction)
    }

    List<LikeInteraction> getAll() {
        return likeInteractionRepository.findAll()
    }

    int countByPrimaryUsernameBetween(String primaryUsername, LocalDateTime start, LocalDateTime end) {
        return likeInteractionRepository.countByPrimaryUsernameBetween(primaryUsername, start, end)
    }

    List<LikeInteraction> getByPrimaryUser(PrimaryUser primaryUser) {
        if (primaryUser == null) {
            return null
        }
        return getByPrimaryUser(primaryUser.username)
    }

    List<LikeInteraction> getByPrimaryUser(String primaryUsername) {
        if (primaryUsername == null) {
            return null
        }
        return likeInteractionRepository.findByPrimaryUsername(primaryUsername)
    }

    LikeInteraction get(String username) {
        if (username == null) {
            return null
        }
        return likeInteractionRepository.findById(username).get()
    }
}
