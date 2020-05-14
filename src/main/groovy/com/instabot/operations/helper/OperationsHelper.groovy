package com.instabot.operations.helper

import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class OperationsHelper {
    private static final Logger LOG = LogManager.getLogger(OperationsHelper.class)
    private static final String INSTAGRAM_PAGE = "https://www.instagram.com/"

    @Autowired
    private InstaWebDriver instaDriver

    /**
     * @param min - minimum number in the random range
     * @param max - minimum number in the random range
     * @return - a random integer between passed min and max
     */
    int getRandomInt(int min, int max) {
        return ((Math.random() * (max - min)) + min) as int
    }

    /**
     * Opens the link of an Instagram user page
     * @param username - the target user whose page should be opened
     */
    void goToUserPage(String username) {
        LOG.info("Go to user page: ${INSTAGRAM_PAGE + username}")
        instaDriver.driver.get(INSTAGRAM_PAGE + username)
    }

    /**
     * Checks if there is a heading with "No Posts Yet" which means that the user hasn't posted anything yet
     * @return true if the heading is found or false otherwise
     * Note: instaWebDriver must be open on an Instagram user page
     */
    boolean userHasNoPosts() {
        List<WebElement> noPostsYetElements = instaDriver.driver.findElements(By.xpath("//h1[contains(text(), 'No Posts Yet')]"))
        return (noPostsYetElements != null && noPostsYetElements.size() != 0)
    }

    /**
     * Opens the first post from an Instagram user page
     * @param username - the target user whose first post should be opened
     */
    void openFirstPost(String username) {
        if (!instaDriver.driver.currentUrl.startsWith(INSTAGRAM_PAGE + username)) {
            goToUserPage(username)
        }

        LOG.debug("Click on the first post image to open it")
        WebElement firstPostelement = getFirstPostElement()
        instaDriver.jse.executeScript("arguments[0].scrollIntoView(true);", firstPostelement)
        instaDriver.actions.moveToElement(firstPostelement).click().perform()
    }

    private WebElement getFirstPostElement() {
        // all posts are stored in the "article" element
        WebElement articleElement = instaDriver.driver.findElement(By.xpath('//article'))

        // all posts are represented by an image, therefore the first image represents the first post
        return articleElement.findElement(By.xpath('.//img'))
    }

    void clickOnWebElement(WebElement webElement) {
        instaDriver.actions.moveToElement(webElement).click().perform()
    }
}
