package com.instabot.operators.weboperations.userextractor

import com.instabot.data.model.user.User
import com.instabot.webdriver.InstaWebDriver
import org.apache.log4j.Logger
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions

import java.time.LocalDateTime

class RelatedUsersUpdater {
    private static final Logger LOG = Logger.getLogger(RelatedUsersUpdater.class)

    /**
     * Extracts all followed and and the users being followd by the master user and updates them in database
     */
    static void updateRelatedUsers(InstaWebDriver instaDriver, String masterUsername) {
        LOG.info("Update related users (followers and following list) for master user: $masterUsername")

        // temporary users holder; it will be replaced by database entity container
        Map<String, User> userMap = new HashMap<>()

        // extract and update users who isFollower for master user
        List<WebElement> followersListElements = getRelatedUsersByType(instaDriver, masterUsername, "followers")
        updateRelatedUsersByType(masterUsername, userMap, followersListElements, "isFollower")

        // extract users who isFollower to master user
        List<WebElement> followingListElements = getRelatedUsersByType(instaDriver, masterUsername, "following")
        updateRelatedUsersByType(masterUsername, userMap, followingListElements, "isFollowed")


        /*
        proof of concept: log all users by type:
        - follower
        - followed
        - followers that are not followed
        - followed that don't follow back
        */
        int i = 1, j = 1, k = 1, u = 1
        userMap.forEach({ id, user ->
            if (user.isFollower) {
                LOG.info "[FOLLOWER] ${i++} User[name=$user.username, description=$user.name, isFollower=$user.isFollower, isFollowed=$user.isFollowed]"
            }
        })
        userMap.forEach({ id, user ->
            if (user.isFollowed) {
                LOG.info "[FOLLOWED] ${j++} User[name=$user.username, description=$user.name, isFollower=$user.isFollower, isFollowed=$user.isFollowed]"
            }
        })
        userMap.forEach({ id, user ->
            if (user.isFollower && !user.isFollowed) {
                LOG.info "[FOLLOWER NOT FOLLOWED] ${k++} User[name=$user.username, description=$user.name, isFollower=$user.isFollower, isFollowed=$user.isFollowed]"
            }
        })
        userMap.forEach({ id, user ->
            if (!user.isFollower && user.isFollowed) {
                LOG.info "[FOLLOWED NOT FOLLOWER]  ${u++} User[name=$user.username, description=$user.name, isFollower=$user.isFollower, isFollowed=$user.isFollowed]"
            }
        })
    }

    /**
     * Goes to master user page, opens Following or Follower (depending on {@param userType} window and extracts all contained users to a list
     *
     * WARNING: sendKeys(Keys.PAGE_DOWN) feature used in this method might not work with other drivers than FireFox driver
     */
    private static List<WebElement> getRelatedUsersByType(InstaWebDriver instaDriver, String masterUsername, String userType) throws InterruptedException {
        // go to user page
        instaDriver.driver.get("https://www.instagram.com/" + masterUsername)

        // find the button linking to the list of users having a specific relationship to the master user (following/follower)
        By usersWindowPath = By.xpath("//a[@href='/" + masterUsername + "/" + userType + "/']")
        instaDriver.wait.until(ExpectedConditions.visibilityOfElementLocated(usersWindowPath))
        WebElement usersWindowButton = instaDriver.driver.findElement(usersWindowPath)

        // get total amount of users based on text format of users qty after removing commas
        int usersTotalAmount = new Integer(getInnerHtml(usersWindowButton.findElement(By.cssSelector("span"))).replaceAll(",", ""))
        LOG.info "Total amount of $userType users: $usersTotalAmount"

        if (usersTotalAmount == 0) {
            return Collections.emptyList()
        }

        // open users (following or followers) window
        usersWindowButton.click()

        // identify the location of the ("following", "followed" or "follow") button of the first user in the list
        By firstUserTypeButtonPath = By.cssSelector("div[role='dialog'] li button")
        instaDriver.wait.until(ExpectedConditions.visibilityOfElementLocated(firstUserTypeButtonPath))
        WebElement firstUserTypeButton = instaDriver.driver.findElement(firstUserTypeButtonPath)

        // navigate 6 steps back through DOM tree to locate the scrolling element - list with all users
        WebElement usersContainer = firstUserTypeButton.findElement(By.xpath("./../../../../../.."))

        // define path of users list menu
        By usersListPath = By.cssSelector("div[role='dialog'] li")

        // scroll down into users list menu until all users are loaded
        List<WebElement> loadedUsers = null
        int loadedUsersAmount = 0
        int lastLoadedUsersAmount = loadedUsersAmount
        while (loadedUsersAmount != usersTotalAmount) {
            // scroll down into users window to load more users
            for (int i in 1..99) {
                usersContainer.sendKeys(Keys.PAGE_DOWN)
            }

            // update users WebElement list and qty
            loadedUsers = instaDriver.driver.findElements(usersListPath)
            loadedUsersAmount = loadedUsers.size()

            LOG.info "$loadedUsersAmount out of $usersTotalAmount users added to WebElement list. Load more users..."

            if (loadedUsersAmount == lastLoadedUsersAmount) {
                throw new Exception("Unable to load all $userType users for $masterUsername master user!")
            }
        }
        LOG.info "All $loadedUsersAmount $userType users added to WebElement list"

        return loadedUsers
    }

    /**
     * For each {@link User} from the map, checks if it already exists, if so, updates its isFollowing or isFollower values based on @{param relationToMasterUser},
     * otherwise adds a new {@link User}
     */

    private static updateRelatedUsersByType(String masterUsername, Map<String, User> userMap, List<WebElement> followersListElements, String relationToMasterUser) {
        for (WebElement userElement : followersListElements) {
            // get links contained in user WebElement
            List<WebElement> linksInUserElement = userElement.findElements(By.cssSelector("a"))

            // find the element that contains title attribute that holds the username
            WebElement userElementWithUsername = null
            for (WebElement linkInUserElement : linksInUserElement) {
                if (linkInUserElement.getAttribute("title") != null)
                    userElementWithUsername = linkInUserElement
            }

            // extract username
            String username = getInnerHtml(userElementWithUsername)

            // find parent element of the username holder - this parent contains another child that holds the name of the user
            WebElement userElementWithUsernameParent = userElementWithUsername.findElement(By.xpath("../../.."))

            // find the second child and extract the name of the user
            List<WebElement> userElementWithUsernameParentsChildren = userElementWithUsernameParent.findElements(By.xpath("./div/div"))
            WebElement userElementWithName = null
            for (WebElement userElementWithUsernameParentsChild : userElementWithUsernameParentsChildren) {
                if (!getInnerHtml(userElementWithUsernameParentsChild).contains("title=\"" + username + "\"")) {
                    // filter the child containing the username
                    userElementWithName = userElementWithUsernameParentsChild
                }
            }

            // extract the name of the user
            String name = getInnerHtml(userElementWithName)

            // if user already exists, update it
            User tempUser = new User(masterUsername, username, name)
            if (userMap.containsKey(tempUser.id)) {
                User user = userMap.get(tempUser.id)

                // set relation to master user
                setUserRelationToMasterUser(user, relationToMasterUser)

                // update name if different
                if (user.name != tempUser.name) {
                    user.setName(tempUser.name)
                }
            } else {
                // set relation to master user
                setUserRelationToMasterUser(tempUser, relationToMasterUser)

                // if user does not exist yet, add it
                userMap.put(tempUser.id, tempUser)
            }
        }
    }

    /**
     * Set isFollowed or isFollowing parameters, based on {@param relationToMasterUser}
     */
    private static void setUserRelationToMasterUser(User user, String relationToMasterUser) {
        if (relationToMasterUser == "isFollower") {
            user.setIsFollower(true)
            user.setIsFollowerLastUpdatedAt(LocalDateTime.now())
        } else {
            user.setIsFollowed(true)
            user.setIsFollowedLastUpdatedAt(LocalDateTime.now())
        }
    }

    /**
     * Extract HTML within {@link WebElement}
     */
    private static String getInnerHtml(WebElement webElement) {
        return webElement.getAttribute("innerHTML")
    }
}
