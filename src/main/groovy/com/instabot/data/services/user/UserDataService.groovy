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
            LOG.debug("Update user $user")
        } else {
            LOG.debug("Create user $user")
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

    List<User> getAllToBeProcessedByMasterUsername(String masterUsername) {
        if (masterUsername == null) {
            return null
        }
        return getAllByMasterUsername(masterUsername).findAll({ user ->
            ((user.nrOfLikes != user.targetedNrOfLikes) || user.targetedNrOfLikes == 0) ||
                    ((user.nrOfComments != user.targetedNrOfComments) || user.targetedNrOfComments == 0)
        })
    }

    List<User> getAllToBeLikedByMasterUsername(String masterUsername) {
        if (masterUsername == null) {
            return null
        }
        LOG.info("Get all users whose posts should be liked by master user: $masterUsername")

        List<User> allRelatedUsers = getAllByMasterUsername(masterUsername)
        LOG.info("Total nr. of related users: ${allRelatedUsers.size()}")

        // TODO && status != processed
        List<User> allRelatedUsersToBeLiked = allRelatedUsers.findAll({ user ->
            (user.nrOfLikes != user.targetedNrOfLikes) || user.targetedNrOfLikes == 0
        })
        LOG.info("Total nr. of users whose posts should be liked: ${allRelatedUsersToBeLiked.size()}")

        return allRelatedUsersToBeLiked
    }

    List<User> getAllToBeCommentedByMasterUsername(String masterUsername) {
        if (masterUsername == null) {
            return null
        }
        return getAllByMasterUsername(masterUsername).findAll({ user ->
            ((user.nrOfComments != user.targetedNrOfComments) || user.targetedNrOfComments == 0)
        })
    }
}
