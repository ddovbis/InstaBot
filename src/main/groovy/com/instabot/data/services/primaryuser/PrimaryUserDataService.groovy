package com.instabot.data.services.primaryuser

import com.instabot.data.model.primaryuser.PrimaryUser
import com.instabot.data.repositories.primaryuser.PrimaryUserRepository
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PrimaryUserDataService {
    private static final Logger LOG = LogManager.getLogger(PrimaryUserDataService.class)

    @Autowired
    private PrimaryUserRepository primaryUserRepository

    PrimaryUser createOrGetIfExists(String primaryUsername) {
        PrimaryUser primaryUser = get(primaryUsername)
        return primaryUser == null ? create(primaryUsername) : primaryUser
    }

    PrimaryUser create(String primaryUsername) {
        PrimaryUser newPrimaryUser = new PrimaryUser(primaryUsername)
        save(newPrimaryUser)
        return newPrimaryUser
    }

    boolean exists(PrimaryUser primaryUser) {
        if (primaryUser == null) {
            return false
        }
        return exists(primaryUser.username)
    }

    boolean exists(String primaryUsername) {
        if (primaryUsername == null) {
            return false
        }
        return primaryUserRepository.existsById(primaryUsername)
    }

    void save(PrimaryUser primaryUser) {
        if (primaryUser == null) {
            throw new NullPointerException("Attempted to save null primary user to database")
        } else if (exists(primaryUser)) {
            LOG.debug("Update primary user $primaryUser")
        } else {
            LOG.debug("Create primary user $primaryUser")
        }
        primaryUserRepository.save(primaryUser)
    }

    void saveAll(List<PrimaryUser> primaryUserList) {
        if (primaryUserList == null) {
            throw new NullPointerException("Attempted to save null primary user list to database")
        }
        LOG.info("Save ${primaryUserList.size()} (crate/update) primary users to database")
        primaryUserRepository.saveAll(primaryUserList)
    }

    List<PrimaryUser> getAll() {
        return primaryUserRepository.findAll()
    }

    private PrimaryUser get(String primaryUsername) {
        if (primaryUsername == null) {
            return null
        }
        return primaryUserRepository.findById(primaryUsername).get()
    }
}
