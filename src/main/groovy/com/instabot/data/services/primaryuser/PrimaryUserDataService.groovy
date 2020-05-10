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
        if (exists(primaryUsername)) {
            return get(primaryUsername)
        } else {
            PrimaryUser newPrimaryUser = new PrimaryUser(primaryUsername)
            save(newPrimaryUser)
            return newPrimaryUser
        }

    }

    boolean exists(PrimaryUser primaryUser) {
        if (PrimaryUser == null) {
            return false
        }
        return exists(primaryUser.username)
    }

    boolean exists(String username) {
        if (username == null) {
            return false
        }
        return primaryUserRepository.existsById(username)
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

    PrimaryUser get(PrimaryUser primaryUserList) {
        if (primaryUserList == null) {
            return null
        }
        return get(primaryUserList.username)
    }

    PrimaryUser get(String username) {
        if (username == null) {
            return null
        }
        return primaryUserRepository.findById(username).get()
    }
}
