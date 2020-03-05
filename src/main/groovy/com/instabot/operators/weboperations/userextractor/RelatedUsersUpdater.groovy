// TODO: long package name
package com.instabot.operators.weboperations.userextractor

// TODO A good indication that this class is doing too many things is in the imports: instabot, jsoup, selenium & spring.
// If you have good separation of concerns, you won't need jsoup and selenium imports here
import com.instabot.data.model.user.User
import com.instabot.data.services.user.UserDataService
import com.instabot.utils.exceptions.user.UsersLoadingException
import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
//TODO This class does too many things. You need to separate html processing from business logic.
// Try to convert html to entities outside this class.
class RelatedUsersUpdater {
    private final Logger LOG = LogManager.getLogger(RelatedUsersUpdater.class)

    @Autowired
    private UserDataService userDataService

    /**
     * Extracts all followed and and the users being followd by the master user and saves or updates them in database
     *
     * @param instaDriver - {@link InstaWebDriver} wrapper with an opened Instagram page which is logged in with an user
     * @param masterUsername - Instagram user whose followers and followed users should be updated; can be different than logged in user @param htmlDocument
     */
    void updateRelatedUsers(InstaWebDriver instaDriver, String masterUsername) {
        LocalDateTime startTime = LocalDateTime.now()

        LOG.info("Start related users (followers and following lists) updater for master user: $masterUsername")
        if (!shouldBeUpdated(masterUsername, startTime)) {
            return
        }

        // extract and update users who isFollower for master user
        String pageSourceWithAllFollowerUsersLoaded = getRelatedUsersByType(instaDriver, masterUsername, "followers")
        updateRelatedUsersByType(pageSourceWithAllFollowerUsersLoaded, masterUsername, "isFollower")

        // extract users who isFollower to master user
        String pageSourceWithAllFollowingUsersLoaded = getRelatedUsersByType(instaDriver, masterUsername, "following")
        updateRelatedUsersByType(pageSourceWithAllFollowingUsersLoaded, masterUsername, "isFollowed")

        // update users that have not been processed during previous steps
        normalizeRelatedUsers(masterUsername, startTime)
    }

    /**
     * @param masterUsername - the main user in relation to whom the rest of the users should be normalized
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
            if (ChronoUnit.DAYS.between(user.getIsFollowedLastUpdatedAt(), startTime) >= 1 || ChronoUnit.DAYS.between(user.getIsFollowerLastUpdatedAt(), startTime) >= 1) {
                LOG.info "At least one related user (${user.username}) has been updated less than 1 day ago; proceed with the update"
                return true
            }
        }
        LOG.info "All users have been updated less than 1 day ago; no update is required"
        return false
    }

    /**
     * Goes to master user page, opens Following or Follower (depending on {@param userType} window and scrolls down until all users are loaded to html
     * @param userType - relationship to the master user (following/follower)
     * @return HTML page source containing all users of specific type
     *
     * WARNING: sendKeys(Keys.PAGE_DOWN) feature used in this method might not work with other drivers than FireFox driver
     */
    private String getRelatedUsersByType(InstaWebDriver instaDriver, String masterUsername, String userType) throws InterruptedException {
        goToUserPage(instaDriver, masterUsername)

        WebElement usersWindowButton = getUsersWindowButton(instaDriver, masterUsername, userType)

        int usersTotalAmount = getUsersTotalAmount(usersWindowButton)
        LOG.info "Load all $userType users for $masterUsername master user;  users to be loaded: $usersTotalAmount"
        if (usersTotalAmount == 0) {
            return Collections.emptyList()
        }

        // open users window, containing a list of users of a certain type (following or followers)
        usersWindowButton.click()

        // identify the first element of the ("following", "followed" or "follow") button of the first user within the open list
        WebElement firstUserTypeButton = getFirstButtonInTheList(instaDriver)

        // navigate 6 steps back through DOM tree to locate the scrolling element - list with all users
        WebElement usersContainer = getParentOfLevel(firstUserTypeButton, 6)

        // define path of users list menu

        // scroll down into users list menu until all users are loaded
        By usersListPath = By.cssSelector("div[role='dialog'] li")
        List<WebElement> loadedUsers
        int loadedUsersAmount = 0
        int lastLoadedUsersAmount = loadedUsersAmount
        int consecutiveFailedLoadingAttempts = 0
        while (loadedUsersAmount != usersTotalAmount) {
            // scroll down into users window to load more users
            scrollDown(usersContainer)

            // update users WebElement list and qty
            loadedUsers = instaDriver.driver.findElements(usersListPath)
            loadedUsersAmount = loadedUsers.size()
            LOG.info "$loadedUsersAmount out of $usersTotalAmount users added to WebElement list. Load more users..."

            // if no users have been loaded since last iteration count a new failed attempt
            if (lastLoadedUsersAmount == loadedUsersAmount) {
                LOG.warn "No new $userType users loaded; consecutive failed loading attempts: ${++consecutiveFailedLoadingAttempts}"
            }

            // after 3 failed attempts return the page or throw an exception if necessary
            if (consecutiveFailedLoadingAttempts == 3) {
                if (isNecessaryToThrowUserLoadingException(loadedUsersAmount, usersTotalAmount)) {
                    throw new UsersLoadingException("Unable to load all $userType users for $masterUsername master user; loaded $loadedUsersAmount out of $usersTotalAmount users")
                } else {
                    LOG.warn "Finish loading $userType users for $masterUsername master user; loaded $loadedUsersAmount users instead of $usersTotalAmount"
                    return instaDriver.driver.getPageSource()
                }
            }

            if (lastLoadedUsersAmount != loadedUsersAmount) {
                lastLoadedUsersAmount = loadedUsersAmount
                consecutiveFailedLoadingAttempts = 0
            }
        }
        LOG.info "All $loadedUsersAmount $userType are loaded"

        return instaDriver.driver.getPageSource()
    }

    /**
     * @param instaDriver - @{link InstaWebDriver} logged in to Instagram
     * @param masterUsername - the target user whose page should be opened
     */
    private void goToUserPage(InstaWebDriver instaDriver, String masterUsername) {
        instaDriver.driver.get("https://www.instagram.com/" + masterUsername)
    }

    /**
     * Finds the button linking to the list of users having a specific relationship to the master user (following/follower)
     *
     * @param instaDriver - {@link InstaWebDriver} logged in to system with an opened master user page
     * @param masterUsername - username whose page is opened
     * @param userType - following/followers
     * @return
     */
    private WebElement getUsersWindowButton(instaDriver, String masterUsername, String userType) {
        By usersWindowPath = By.xpath("//a[@href='/" + masterUsername + "/" + userType + "/']")
        instaDriver.wait.until(ExpectedConditions.visibilityOfElementLocated(usersWindowPath))
        return instaDriver.driver.findElement(usersWindowPath)
    }

    /**
     * @param usersWindowButton - {@link WebElement} containing total amount of user of a certain type
     * @return - total amount of users based on text format of users qty after removing commas
     */
    private Integer getUsersTotalAmount(WebElement usersWindowButton) {
        return new Integer(getInnerHtml(usersWindowButton.findElement(By.cssSelector("span"))).replaceAll(",", ""))
    }

    /**
     * Extract HTML within {@link WebElement}
     */
    private String getInnerHtml(WebElement webElement) {
        return webElement.getAttribute("innerHTML")
    }

    /**
     * @param instaDriver - {@link InstaWebDriver} containing a list with buttons
     * @return - the first button in the list
     */
    private WebElement getFirstButtonInTheList(InstaWebDriver instaDriver) {
        By firstUserTypeButtonPath = By.cssSelector("div[role='dialog'] li button")
        instaDriver.wait.until(ExpectedConditions.visibilityOfElementLocated(firstUserTypeButtonPath))
        return instaDriver.driver.findElement(firstUserTypeButtonPath)
    }

    /**
     * @param webElement - child element
     * @param parentLevel - how many times to navigate back through DOM tree
     * @return - element found after navigating {@param parentLevel} times back through DOM tree
     */
    private WebElement getParentOfLevel(WebElement webElement, int parentLevel) {
        // append "../" for each parent level
        String xpath = "./" + ("../" * parentLevel)
        // remove last character to have a valid xpath ('/')
        xpath = xpath[0..xpath.length() - 2]
        return webElement.findElement(By.xpath(xpath))
    }

    /**
     * Imitates pressing PAGE_DOWN key for multiple times
     *
     * @param elementToScroll - scrollable {@link WebElement}
     */
    private void scrollDown(WebElement elementToScroll) {
        for (int i in 1..100) {
            elementToScroll.sendKeys(Keys.PAGE_DOWN)
        }
    }

    /**
     * @param loadedUsersAmount - the amount of users loaded to html
     * @param usersTotalAmount - how many users are expected to be loaded to html
     * @return - false if difference between {@param loadedUsersAmountsers} and {@param usersTotalAmount) is less than 5, as it could be due to wrong total users number on Instagram page
     *         - true otherwise
     */
    private boolean isNecessaryToThrowUserLoadingException(int loadedUsersAmount, int usersTotalAmount) {
        if (Math.abs(loadedUsersAmount - usersTotalAmount) < 5) {
            return false
        }
        return true
    }

/**
 * Updates {@link User#name} and {@link User#isFollowed} or {@link User#isFollower} parameters
 * based on {@param relationToMasterUser}, for all {@link User}s found in {@param pageSource}
 *
 * @param pageSource - page source with loaded {@param relationToMasterUser} users
 * @param masterUsername - user whose page is loaded in {@link InstaWebDriver}
 * @param relationToMasterUser - isFollower or isFollowed
 */
    private void updateRelatedUsersByType(String pageSource, String masterUsername, String relationToMasterUser) {
        LOG.info("Save loaded users to database")
        for (Element singleUserContainer : getSingleUserContainerList(pageSource)) {
            // extract username from link's text (ignore elements with images)
            Element userLink = singleUserContainer.select("a[href]:not(:has(img))").first()
            String username = userLink.text()
            // get userLink's "brother" element, which contains user's name (ignore elements containing title attribute)
            Element userLinkParent = userLink.parent().parent()
            String name = userLinkParent.select("div:not(:has([title]))").first().html()

            User tempUser = new User(masterUsername, username, name)
            setUserRelationToMasterUser(tempUser, relationToMasterUser)
            pushUserToDataService(tempUser)
        }
    }

    /**
     * @param pageSource - html page source containing an open "Following" or "Followers" users window
     * @return html list with all "Following" or "Followers" users
     */
    Elements getSingleUserContainerList(String pageSource) {
        Document htmlDocument = Jsoup.parse(pageSource)
        // find first button (expected to be in the same div with the first user)
        Element firstButton = htmlDocument.select("div[role='dialog'] li button").first()
        // navigate 6 steps back through DOM tree to locate the scrolling element - list with all users
        Element usersContainer = firstButton.parent().parent().parent().parent().parent().parent()
        // extract the list containing information about each individual user
        return usersContainer.select("li")
    }

    /**
     * If {@link User} exists {@link UserDataService}, updates {@link User#name} and {@link User#isFollowed} or {@link User#isFollower} parameters based on {@param relationToMasterUser},
     * otherwise creates a new {@link User} via {@link UserDataService}
     *
     * @param tempUser - basic {@link User} containing {@link User#id}, {@link User#masterUsername}, {@link User#username}, and {@link User#name}
     */
    private void pushUserToDataService(User tempUser) {
        if (userDataService.exists(tempUser)) {
            User user = userDataService.get(tempUser)
            boolean shouldBeUpdated = false

            if (tempUser.isFollower != user.isFollower || user.isFollowerLastUpdatedAt == null) {
                user.setIsFollower(tempUser.isFollower)
                shouldBeUpdated = true
            }
            if (tempUser.isFollowed != user.isFollowed || user.isFollowedLastUpdatedAt == null) {
                user.setIsFollowed(tempUser.isFollowed)
                shouldBeUpdated = true
            }
            if (tempUser.name != user.name) {
                user.setName(tempUser.name)
                shouldBeUpdated = true
            }

            if (shouldBeUpdated) {
                userDataService.save(user)
            }
        } else {
            userDataService.save(tempUser)
        }
    }


/**
 * Sets isFollowed or isFollowing parameters, based on {@param relationToMasterUser}
 */
    private void setUserRelationToMasterUser(User user, String relationToMasterUser) {
        if (relationToMasterUser == "isFollower") {
            user.setIsFollower(true)
        } else {
            user.setIsFollowed(true)
        }
    }

    /**
     * Sets {@link User#isFollower} and {@link User#isFollowed} parameters for all users who have not been processed during the last related users update
     *
     * @param masterUsername - the main user in relation to whom the rest of the users should be normalized
     * @param startTime - the time when started last users update
     */
    private void normalizeRelatedUsers(String masterUsername, LocalDateTime startTime) {
        for (User user : userDataService.getAllByMasterUsername(masterUsername)) {
            boolean shouldBeUpdated = false
            if (user.isFollowerLastUpdatedAt == null || (user.isFollowerLastUpdatedAt < startTime)) {
                user.setIsFollower(false)
                shouldBeUpdated = true
            }
            if (user.isFollowedLastUpdatedAt == null || (user.isFollowedLastUpdatedAt < startTime)) {
                user.setIsFollowed(false)
                shouldBeUpdated = true
            }
            if (shouldBeUpdated) {
                userDataService.save(user)
            }
        }
    }
}
