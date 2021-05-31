package com.instabot.operations.updater.relatedusers

import com.instabot.operations.helper.OperationsHelper
import com.instabot.utils.exceptions.user.UsersLoadingException
import com.instabot.webdriver.InstaWebDriver
import com.instabot.webdriver.WebDriverHelper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RelatedUsersPageLoader {
    private final Logger LOG = LogManager.getLogger(RelatedUsersPageLoader.class)
    private final Random RANDOM = new Random()

    @Autowired
    private InstaWebDriver instaDriver
    @Autowired
    private WebDriverHelper driverHelper
    @Autowired
    private OperationsHelper operationsHelper

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
                LOG.warn("Failed to load all $userType users for $masterUsername master user; try again in safe-mode (slower loading)")
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
        LOG.info("Load all $userType users for $masterUsername master user;  users to be loaded: $usersTotalAmount")
        if (usersTotalAmount == 0) {
            LOG.info("No $userType users to be loaded")
            return
        }

        // open users window, containing a list of users of a certain type (following or followers)
        usersWindowButton.click()
        instaDriver.wait10sec.until {
            !getLoadedUsers().empty
        }

        loadAllUsers(masterUsername, userType, safeMode, usersTotalAmount)
    }

    void loadAllUsers(String masterUsername, String userType, boolean safeMode, int usersTotalAmount) {
        int loadedUsersAmount = 0
        int lastLoadedUsersAmount = 0
        int consecutiveFailedLoadingAttempts = 0
        while (loadedUsersAmount != usersTotalAmount) {
            // update users WebElement list and qty
            List<WebElement> loadedUsers = loadMoreUsers(safeMode)
            loadedUsersAmount = loadedUsers.size()
            LOG.debug("Loaded $loadedUsersAmount out of $usersTotalAmount '$userType' users; continue loading")

            if (lastLoadedUsersAmount != loadedUsersAmount) {
                lastLoadedUsersAmount = loadedUsersAmount
                consecutiveFailedLoadingAttempts = 0
            } else {
                // if no users have been loaded since last iteration count a new failed attempt
                LOG.warn("No new $userType users loaded; consecutive failed loading attempts: ${++consecutiveFailedLoadingAttempts}")
            }

            // after 3 failed attempts return the page or throw an exception if necessary
            if (consecutiveFailedLoadingAttempts == 5) {
                if (isNecessaryToThrowUserLoadingException(loadedUsersAmount, usersTotalAmount)) {
                    throw new UsersLoadingException("Unable to load all $userType users for $masterUsername master user; loaded $loadedUsersAmount out of $usersTotalAmount users")
                } else {
                    LOG.warn("Finish loading $userType users for $masterUsername master user; loaded $loadedUsersAmount users instead of $usersTotalAmount")
                    return
                }
            }
        }
        LOG.info("All $loadedUsersAmount $userType are loaded")
    }

    List<WebElement> getLoadedUsers() {
        return driverHelper.getAllByCssSelector("div[role='dialog'] li")
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
        instaDriver.wait10sec.until(ExpectedConditions.visibilityOfElementLocated(usersWindowPath))
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
        instaDriver.wait10sec.until(ExpectedConditions.visibilityOfElementLocated(firstUserTypeButtonPath))
        return instaDriver.driver.findElement(firstUserTypeButtonPath)
    }

    /**
     * @param safeMode - if enabled, adds sleep time before iteration and holds PAGE_DOWN key for a shorter period
     * Note: In case StaleElementReferenceException is caught, it will be ignored
     * the calling method must handle the issue based on the fact that there are no new users loaded
     */
    private List<WebElement> loadMoreUsers(boolean safeMode) {
        long minSleepTime = safeMode ? 250 : 50
        long maxSleepTime = safeMode ? 1000 : 250
        long sleepTime = Math.abs(RANDOM.nextInt() % (maxSleepTime - minSleepTime)) + minSleepTime

        // sleep between min and max time
        sleep(sleepTime)

        try {
            driverHelper.scrollToElement(getLoadedUsers().last())
        } catch (StaleElementReferenceException ignore) {
        }

        sleep(sleepTime)
        return getLoadedUsers()
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