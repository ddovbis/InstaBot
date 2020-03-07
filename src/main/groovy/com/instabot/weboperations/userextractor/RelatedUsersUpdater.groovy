package com.instabot.weboperations.userextractor

import com.instabot.data.model.user.User
import com.instabot.data.services.user.UserDataService

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class RelatedUsersUpdater {
    private final Logger LOG = LogManager.getLogger(RelatedUsersUpdater.class)

    @Autowired
    private UserDataService userDataService
    @Autowired
    private RelatedUsersPageLoader relatedUsersPageLoader
    @Autowired
    private RelatedUsersExtractor relatedUsersExtractor

    /**
     * Extracts all followed and and the users being followd by the master user and saves or updates them in database
     *
     * @param masterUsername - Instagram user whose followers and followed users should be updated; can be different than logged in user @param htmlDocument
     */
    void updateRelatedUsers(String masterUsername) {
        LocalDateTime startTime = LocalDateTime.now()

        LOG.info("Start related users (followers and following lists) updater for master user: $masterUsername")
        if (!shouldBeUpdated(masterUsername, startTime)) {
            return
        }

        Map<String, User> extractedUserIdToUserMap = new HashMap<>()

        // extract and update users who isFollower for master user
        String pageSourceWithAllFollowerUsersLoaded = relatedUsersPageLoader.getPageSourceWithLoadedUsersByType(masterUsername, "followers")
        relatedUsersExtractor.extractRelatedUsersFromPageSource(extractedUserIdToUserMap, pageSourceWithAllFollowerUsersLoaded, masterUsername, "isFollower")

        // extract users who isFollower to master user
        String pageSourceWithAllFollowingUsersLoaded = relatedUsersPageLoader.getPageSourceWithLoadedUsersByType(masterUsername, "following")
        relatedUsersExtractor.extractRelatedUsersFromPageSource(extractedUserIdToUserMap, pageSourceWithAllFollowingUsersLoaded, masterUsername, "isFollowed")

        // update stored users
        Map<String, User> updatedUserIdToUserMap = getMapWithUpdatedStoredUsers(extractedUserIdToUserMap, masterUsername)

        // add new users to be saved
        addMissingUsersToMap(updatedUserIdToUserMap, extractedUserIdToUserMap)

        // update users in dataservice
        List<User> updatedUsers = updatedUserIdToUserMap.values().collect()
        userDataService.saveAll(updatedUsers)
    }

    /**
     * @param masterUsername - main user in relation to whom the rest of the users should be normalized
     * @return - true if any of related users have been updated more than one day ago, or false otherwise
     */
    private boolean shouldBeUpdated(String masterUsername, LocalDateTime startTime) {
        LOG.info "Check if related users should be updated"
        List<User> allUsers = userDataService.getAllByMasterUsername(masterUsername)
        if (allUsers.isEmpty()) {
            LOG.info "No users found in data service; proceed with the update"
            return true
        }

        for (User user : allUsers) {
            if (ChronoUnit.DAYS.between(user.getIsFollowedLastUpdatedAt(), startTime) >= 0 || ChronoUnit.DAYS.between(user.getIsFollowerLastUpdatedAt(), startTime) >= 0) {
                LOG.info "At least one related user (${user.username}) has been updated less than 1 day ago; proceed with the update"
                return true
            }
        }
        LOG.info "All users have been updated less than 1 day ago; no update is required"
        return false
    }

    /**
     * @param extractedUserIdToUserMap - users extracted from the following/followers list, the stored users shall be updated based on them
     * @param masterUsername - main user in relation to whom the rest of the users should be updated
     * @return - a {@link Map<String, User>} containing stored users with updated {@link User#name}, {@link User#isFollower},
     * and {@link User#isFollowed} based on the information extracted in {@param extractedUserIdToUserMap}
     */
    private Map<String, User> getMapWithUpdatedStoredUsers(Map<String, User> extractedUserIdToUserMap, String masterUsername) {
        List<User> storedUsersRelatedToMasterUser = userDataService.getAllByMasterUsername(masterUsername)
        Map<String, User> updatedUserIdToUserMap = new HashMap<>()

        storedUsersRelatedToMasterUser.each { user ->
            if (extractedUserIdToUserMap.containsKey(user.id)) {
                User extractedUser = extractedUserIdToUserMap.get(user.id)
                user.setName(extractedUser.name)
                user.setIsFollowed(extractedUser.isFollowed)
                user.setIsFollower(extractedUser.isFollower)
            } else {
                user.setIsFollower(false)
                user.setIsFollowed(false)
            }
            updatedUserIdToUserMap.put(user.id, user)
        }

        return updatedUserIdToUserMap
    }

    /**
     * Adds all users from {@param extractedUserIdToUserMap} to {@param toBeUpdatedUserIdToUserMap}, if they do not already exists there
     * @param toBeUpdatedUserIdToUserMap a {@link Map} with all users that shall be updated
     * @param extractedUserIdToUserMap users extracted from the web page
     */
    private void addMissingUsersToMap(Map<String, User> toBeUpdatedUserIdToUserMap, Map<String, User> extractedUserIdToUserMap) {
        extractedUserIdToUserMap.each { userId, user ->
            if (!toBeUpdatedUserIdToUserMap.containsKey(user.id)) {
                toBeUpdatedUserIdToUserMap.put(userId, user)
            }
        }
    }
}