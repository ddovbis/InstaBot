package com.instabot.data.services.user


import com.instabot.config.InstaBotConfig
import com.instabot.data.model.user.User
import com.instabot.data.repositories.user.UserRepository
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

@Service
class UserDataService {
    private static final Logger LOG = LogManager.getLogger(UserDataService.class)

    @Autowired
    private InstaBotConfig instaBotConfig
    @Autowired
    private UserRepository userRepository

    private boolean doNotLikeFollowers

    @Bean("initializeUserDataService")
    void initialize() {
        LOG.info("Initialize UserDataService")
        doNotLikeFollowers = instaBotConfig.getIniFile().get("user-liker", "do-not-like-followers", Boolean.class) as boolean
        LOG.info("set do-not-like-followers: $doNotLikeFollowers")
    }

    boolean exists(User user) {
        if (user == null) {
            return false
        }
        return exists(user.id)
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

    void saveAll(List<User> userList) {
        if (userList == null) {
            throw new NullPointerException("Attempted to save null user list to database")
        }
        LOG.info("Save ${userList.size()} (crate/update) users to database")
        userRepository.saveAll(userList)
    }

    List<User> getAll() {
        return userRepository.findAll()
    }

    User get(User user) {
        if (user == null) {
            return null
        }
        return get(user.id)
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

    List<User> getAllToBeLikedByMasterUsername(String masterUsername) {
        if (masterUsername == null) {
            return null
        }
        LOG.info("Get all users whose posts should be liked by master user: $masterUsername")

        List<User> allRelatedUsers = getAllByMasterUsername(masterUsername)
        LOG.info("Total nr. of related users: ${allRelatedUsers.size()}")

        List<User> allRelatedUsersToBeLiked = allRelatedUsers.findAll({ user ->
            !user.isFullyLiked() && (!doNotLikeFollowers || (doNotLikeFollowers && !user.isFollower) && user.isFollowed)
        })
        LOG.info("Total nr. of users whose posts should be liked: ${allRelatedUsersToBeLiked.size()}")

        return allRelatedUsersToBeLiked
    }
}
