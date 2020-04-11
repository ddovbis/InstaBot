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
     * Opens the first post from an Instagram user page
     * @param username - the target user whose first post should be opened
     */
    void openFirstPost(String username) {
        goToUserPage(username)

        LOG.debug("Click on the first post image to open it")
        instaDriver.actions.moveToElement(getFirstPostElement()).click().perform()
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
