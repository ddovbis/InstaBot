package com.instabot.operations.userextractor.relatedusers

import com.instabot.config.InstaBotConfig
import com.instabot.data.model.user.User
import com.instabot.data.services.primaryuser.PrimaryUserDataService
import com.instabot.data.services.user.UserDataService
import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class RelatedUsersUpdater {
    private final Logger LOG = LogManager.getLogger(RelatedUsersUpdater.class)

    @Autowired
    private InstaBotConfig initializeInstaBotConfig
    @Autowired
    private UserDataService userDataService
    @Autowired
    private InstaWebDriver instaWebDriver
    @Autowired
    private RelatedUsersPageLoader relatedUsersPageLoader
    @Autowired
    private RelatedUsersExtractor relatedUsersExtractor
    @Autowired
    PrimaryUserDataService primaryUserDataService

    private Integer updateFrequency

    @Bean("initializeRelatedUsersUpdater")
    @DependsOn("initializeInstaBotConfig")
    private void initialize() {
        LOG.info("Initialize RelatedUsersUpdater")
        updateFrequency = initializeInstaBotConfig.getIniFile().get("related-users", "update-frequency", Integer.class)
    }

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

        // set isFollower and isFollowed to 'false' if the value has not been updated since starting users-updating process
        normalizeUsers(extractedUserIdToUserMap, startTime)

        // update stored users
        Map<String, User> toBeUpdatedUserIdToUserMap = getMapWithUpdatedStoredUsers(extractedUserIdToUserMap, masterUsername)
        int storedUsersToBeUpdated = toBeUpdatedUserIdToUserMap.size()
        LOG.info("Added $storedUsersToBeUpdated stored related users to the updater container")

        // add new users to be saved
        addMissingUsersToMap(toBeUpdatedUserIdToUserMap, extractedUserIdToUserMap)
        LOG.info("Added ${toBeUpdatedUserIdToUserMap.size() - storedUsersToBeUpdated} new related users to the updater container")

        // update users in dataservice
        List<User> updatedUsers = toBeUpdatedUserIdToUserMap.values().collect()
        userDataService.saveAll(updatedUsers)

        updatePrimaryUserStats(masterUsername, updatedUsers)
    }

    /**
     * @param masterUsername - main user in relation to whom the rest of the users should be normalized
     * @return - true if any of related users have been updated more than one day ago, or false otherwise
     */
    private boolean shouldBeUpdated(String masterUsername, LocalDateTime startTime) {
        LOG.info("Check if related users should be updated")

        if (updateFrequency == null || updateFrequency == 0) {
            LOG.info("No related-users updater-frequency is set; updated will not be performed")
        }

        if (masterUsername == instaWebDriver.primaryUsername) {
            return shouldBeUpdatedStandardMode(startTime)
        } else {
            return shouldBeUpdatedReportingMode(masterUsername, startTime)
        }
    }

    private boolean shouldBeUpdatedStandardMode(LocalDateTime startTime) {
        LocalDateTime lastRelatedUsersUpdate = instaWebDriver.primaryUser.relatedUsersUpdatedAt
        if (lastRelatedUsersUpdate == null) {
            LOG.info("Primary user's related users have never been updated; proceed with the update")
            return true
        } else if (ChronoUnit.MINUTES.between(instaWebDriver.primaryUser.relatedUsersUpdatedAt, startTime) >= updateFrequency) {
            LOG.info("Primary user's related users have been updated more than $updateFrequency minute(s) ago; proceed with the update")
            return true
        } else {
            LOG.info("Primary user's related users have been updated less than $updateFrequency minute(s) ago; no update is required")
            return false
        }
    }

    private boolean shouldBeUpdatedReportingMode(String masterUsername, LocalDateTime startTime) {
        List<User> allUsers = userDataService.getAllByMasterUsername(masterUsername)
        if (allUsers == null || allUsers.isEmpty()) {
            LOG.info("No users found in data service; proceed with the update")
            return true
        }

        for (User user : allUsers) {
            if (ChronoUnit.MINUTES.between(user.getIsFollowedLastUpdatedAt(), startTime) >= updateFrequency || ChronoUnit.MINUTES.between(user.getIsFollowerLastUpdatedAt(), startTime) >= updateFrequency) {
                LOG.info("At least one related user (${user.username}) has been updated less than $updateFrequency minute(s) ago; proceed with the update")
                return true
            }
        }
        LOG.info("All users have been updated less than $updateFrequency minute(s) ago; no update is required")
        return false
    }

    /**
     * If isFollowed or isFollower last updated time is less than {@param startTime}, it means that the user is not
     * in the specified list anymore, so the value should be set to false
     *
     * @param toBeNormalizedUserIdToUserMap - map containing users that should be normalized
     * @param startTime - time when users-update process started
     */
    private void normalizeUsers(Map<String, User> toBeNormalizedUserIdToUserMap, LocalDateTime startTime) {
        LOG.info("Normalize, if necessary, isFollower and isFollowing parameters for ${toBeNormalizedUserIdToUserMap.size()} users")

        boolean isNormalized = false
        int normalizedUsers = 0
        toBeNormalizedUserIdToUserMap.each { userId, user ->
            isNormalized = false
            if (user.isFollowedLastUpdatedAt == null || user.isFollowedLastUpdatedAt < startTime) {
                user.setIsFollowed(false)
                isNormalized = true
            }
            if (user.isFollowerLastUpdatedAt == null || user.getIsFollowerLastUpdatedAt() < startTime) {
                user.setIsFollower(false)
                isNormalized = true
            }
            if (isNormalized) {
                normalizedUsers++
            }
        }
        LOG.info("Normalized $normalizedUsers users")
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

    /**
     * Updates primary user with the total number of followers, followed, and the timestamp of the last related users update
     */
    private void updatePrimaryUserStats(String masterUsername, List<User> updatedUsers) {
        LOG.debug("Update primary users stats (followers, followed, and the timestamp of the last related users update)")
        if (masterUsername != instaWebDriver.primaryUsername) {
            LOG.debug("InstaBot is running in reporting-mode; no update is required")
        }
        instaWebDriver.primaryUser.followers = updatedUsers.findAll(user -> user.isFollower).size()
        instaWebDriver.primaryUser.following = updatedUsers.findAll(user -> user.isFollowed).size()
        instaWebDriver.primaryUser.relatedUsersUpdatedAt = LocalDateTime.now()
        primaryUserDataService.save(instaWebDriver.primaryUser)
    }
}