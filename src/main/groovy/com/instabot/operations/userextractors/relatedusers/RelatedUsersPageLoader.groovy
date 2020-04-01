package com.instabot.operations.userextractors.relatedusers

import com.instabot.operations.helper.OperationsHelper
import com.instabot.utils.exceptions.user.UsersLoadingException
import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RelatedUsersPageLoader {
    private final Logger LOG = LogManager.getLogger(RelatedUsersPageLoader.class)
    private final Random RANDOM = new Random()

    @Autowired
    InstaWebDriver instaDriver
    @Autowired
    OperationsHelper operationsHelper

    /**
     * Goes to master user page, opens Following or Follower (depending on {@param userType} window and scrolls down until all users are loaded to html
     * @param userType - relationship to the master user (following/follower)
     * @return HTML page source containing all users of specific type
     *
     * WARNING: sendKeys(Keys.PAGE_DOWN) feature used in this method might not work with other drivers than FireFox driver
     */
    String getPageSourceWithLoadedUsersByType(String masterUsername, String userType) throws InterruptedException {
        int criticalFailedLoadingAttempts = 0
        try {
            loadUsers(masterUsername, userType, false)
        } catch (UsersLoadingException ule) {
            if (criticalFailedLoadingAttempts == 0) {
                criticalFailedLoadingAttempts++
                LOG.warn "Failed to load all $userType users for $masterUsername master user; try again in safe-mode (slower loading)"
                loadUsers(masterUsername, userType, true)
            } else {
                throw ule
            }
        }

        return instaDriver.driver.getPageSource()
    }

    private void loadUsers(String masterUsername, String userType, boolean safeMode) {
        operationsHelper.goToUserPage(masterUsername)

        WebElement usersWindowButton = getUsersWindowButton(masterUsername, userType)

        int usersTotalAmount = getUsersTotalAmount(usersWindowButton)
        LOG.info "Load all $userType users for $masterUsername master user;  users to be loaded: $usersTotalAmount"
        if (usersTotalAmount == 0) {
            LOG.info("No $userType users to be loaded")
            return
        }

        // open users window, containing a list of users of a certain type (following or followers)
        usersWindowButton.click()

        // identify the first element of the ("following", "followed" or "follow") button of the first user within the open list
        WebElement firstUserTypeButton = getFirstButtonInTheList()

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
            scrollDown(usersContainer, safeMode)

            // update users WebElement list and qty
            loadedUsers = instaDriver.driver.findElements(usersListPath)
            loadedUsersAmount = loadedUsers.size()
            LOG.info "Loaded $loadedUsersAmount out of $usersTotalAmount '$userType' users; continue loading"

            // if no users have been loaded since last iteration count a new failed attempt
            if (lastLoadedUsersAmount == loadedUsersAmount) {
                LOG.warn "No new $userType users loaded; consecutive failed loading attempts: ${++consecutiveFailedLoadingAttempts}"
            }

            // after 3 failed attempts return the page or throw an exception if necessary
            if (consecutiveFailedLoadingAttempts == 5) {
                if (isNecessaryToThrowUserLoadingException(loadedUsersAmount, usersTotalAmount)) {
                    throw new UsersLoadingException("Unable to load all $userType users for $masterUsername master user; loaded $loadedUsersAmount out of $usersTotalAmount users")
                } else {
                    LOG.warn "Finish loading $userType users for $masterUsername master user; loaded $loadedUsersAmount users instead of $usersTotalAmount"
                    return
                }
            }

            if (lastLoadedUsersAmount != loadedUsersAmount) {
                lastLoadedUsersAmount = loadedUsersAmount
                consecutiveFailedLoadingAttempts = 0
            }
        }
        LOG.info "All $loadedUsersAmount $userType are loaded"
    }

    /**
     * Finds the button linking to the list of users having a specific relationship to the master user (following/follower)
     *
     * @param instaDriver - {@link InstaWebDriver} logged in to system with an opened master user page
     * @param masterUsername - username whose page is opened
     * @param userType - following/followers
     * @return
     */
    private WebElement getUsersWindowButton(String masterUsername, String userType) {
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
    private WebElement getFirstButtonInTheList() {
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
     * @param safeMode - if enabled, adds sleep time before iteration and holds PAGE_DOWN key for a shorter period
     */
    private void scrollDown(WebElement elementToScroll, boolean safeMode) {
        if (safeMode) {
            // sleep between 0.5 and 2 seconds
            sleep(Math.abs(RANDOM.nextInt() % (2000 - 500)) + 500)
            // send PAGE_DOWN key 10 times (imitate short hold)
            for (int i in 1..10) {
                elementToScroll.sendKeys(Keys.PAGE_DOWN)
            }
        } else {
            // send PAGE_DOWN key 100 times (imitate long hold)
            for (int i in 1..100) {
                elementToScroll.sendKeys(Keys.PAGE_DOWN)
            }
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
}
