package com.instabot.data.services.user

import com.instabot.data.model.user.User
import com.instabot.data.repositories.user.UserRepository
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserDataService {
    private static final Logger LOG = LogManager.getLogger(UserDataService.class)

    @Autowired
    private UserRepository userRepository

    boolean exists(User user) {
        if (user == null) {
            return false
        }
        return exists(user.getId())
    }

    boolean exists(String id) {
        if (id == null) {
            return false
        }
        return userRepository.existsById(id)
    }

    void save(User user) {
        if (user == null) {
            throw new NullPointerException("Attempted to save null user to database")
        } else if (exists(user)) {
            LOG.debug "Update user $user"
        } else {
            LOG.debug "Create user $user"
        }
        userRepository.save(user)
    }

    void saveAll(List<User> users) {
        if (users == null) {
            throw new NullPointerException("Attempted to save null user list to database")
        }
        LOG.info("Save ${users.size()} (crate/update) users to database")
        userRepository.saveAll(users)
    }

    List<User> getAll() {
        return userRepository.findAll()
    }

    User get(User user) {
        if (user == null) {
            return null
        }
        return get(user.getId())
    }

    User get(String id) {
        if (id == null) {
            return null
        }
        return userRepository.findById(id).get()
    }

    List<User> getAllByMasterUsername(String masterUsername) {
        if (masterUsername == null) {
            return null
        }
        return userRepository.findByMasterUsername(masterUsername)
    }
}
