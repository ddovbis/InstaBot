package com.instabot.operations.liker.relatedusers

import com.instabot.config.InstaBotConfig
import com.instabot.data.model.user.User
import com.instabot.data.model.user.UserLabel
import com.instabot.data.services.user.UserDataService
import com.instabot.operations.helper.OperationsHelper
import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

/*
    TODO
        1. LOG (in debug) the stand by time
        2. Random() sets multiple times in a raw the same number. Solve it.
 */

@Component
class RelatedUsersLiker {
    private static final Logger LOG = LogManager.getLogger(RelatedUsersLiker.class)

    @Autowired
    private InstaBotConfig instaBotConfig
    @Autowired
    private UserDataService userDataService
    @Autowired
    private OperationsHelper operationsHelper
    @Autowired
    private InstaWebDriver instaDriver

    private int targetedNrOfLikesMin
    private int targetedNrOfLikesMax

    @Bean("initializeRelatedUsersLiker")
    private void initialize() {
        LOG.info("Initialize RelatedUsersLiker")

        targetedNrOfLikesMin = instaBotConfig.getIniFile().get("related-users", "targeted-nr-of-likes-min", Integer.class)
        LOG.info("Min. targeted nr. of likes: $targetedNrOfLikesMin")

        targetedNrOfLikesMax = instaBotConfig.getIniFile().get("related-users", "targeted-nr-of-likes-max", Integer.class)
        LOG.info("Max. targeted nr. of likes: $targetedNrOfLikesMax")
    }

    void likeRelatedUsersPosts() {
        LOG.info("Start processing post likes for users related to master user: $instaDriver.primaryUsername")

        List<User> userToBeLikedList = userDataService.getAllToBeLikedByMasterUsername(instaDriver.primaryUsername)
        for (User userToBeLiked : userToBeLikedList) {
            likeUserPosts(userToBeLiked)
            sleep(operationsHelper.getRandomInt(18, 36) * 1000)
        }
    }

    void likeUserPosts(User user) {
        LOG.info("Start processing post likes for user: $user.username")

        setTargetNrOfLikes(user)

        if (user.isFullyLiked()) {
            LOG.info("User $user.username is fully liked ($user.nrOfLikes out of $user.targetedNrOfLikes posts liked); no post likes processing is required")
            return
        }

        operationsHelper.goToUserPage(user.username)

        if (operationsHelper.userHasNoPosts()) {
            LOG.info("User has no posts")
            setAsFullyLiked(user)
            return
        }

        operationsHelper.openFirstPost(user.username)

        // TODO to  a method
        int postNr = 0
        WebElement nextPostButtonElement
        do {
            WebElement likeButtonSvgElement = getLikeButtonSvgElement()
            boolean isPostLiked = isPostLiked(likeButtonSvgElement)

            if (isPostLiked) {
                LOG.info("Post nr. $postNr has already been liked")
                setAsFullyLiked(user)
                return
            }

            // TODO To a method
            LOG.info("Like post nr: ${++postNr}")
            sleep(operationsHelper.getRandomInt(2, 6) * 1000) // TODO implement waiting time based on .ini
            instaDriver.actions.moveToElement(likeButtonSvgElement).click().perform()
            user.incrementNrOfLikes()
            LOG.info("total posts liked: $user.nrOfLikes out of $user.targetedNrOfLikes")
            userDataService.save(user) // TODO report an ActionLike instead


            // TODO implement waiting time based on .ini; move waiting time
            sleep(operationsHelper.getRandomInt(18, 24) * 1000)


            nextPostButtonElement = getNextPostButtonElement()
            if (nextPostButtonElement == null) {
                LOG.info("User $user.username has no more posts")
                setAsFullyLiked(user)
                return
            }

            // move to the next post
            operationsHelper.clickOnWebElement(nextPostButtonElement)
        } while (!user.isFullyLiked())
    }

    private void setTargetNrOfLikes(User user) {
        if (user.targetedNrOfLikes == 0 || user.targetedNrOfLikes < targetedNrOfLikesMin || user.targetedNrOfLikes > targetedNrOfLikesMax) {
            // set a random nr. between minimum and maximum nr. of targeted likes
            user.targetedNrOfLikes = operationsHelper.getRandomInt(targetedNrOfLikesMin, targetedNrOfLikesMax)
            LOG.info("Targeted nr. of likes for user $user.username was set to: $user.targetedNrOfLikes")
            userDataService.save(user)
        }
    }

    private void setAsFullyLiked(User user) {
        LOG.info("Add user label: FULLY_LIKED")
        user.addLabel(UserLabel.FULLY_LIKED)
        userDataService.save(user)
    }

    /**
     * @return - svg element contained in the "Like" button, which also can be used as a reference to click the button
     * Note: <svg> tag is used as a container for SVG graphics
     */
    private WebElement getLikeButtonSvgElement() {
        // case-insensitive selector; aria-label ends in "like" (Like or Unlike)
        By likeButtonSvgCssSelector = By.cssSelector("svg[aria-label*='like' i]")
        instaDriver.wait.until(ExpectedConditions.visibilityOfElementLocated(likeButtonSvgCssSelector))
        return instaDriver.driver.findElement(likeButtonSvgCssSelector)
    }

    /**
     *
     * @param likeButtonSvgElement - a {@link WebElement} containing 'aria-label' attribute with 'Unlike' value if
     * the post has already been like or 'Like' value otherwise
     * @return - true if the post has been already like and false otherwise
     */
    private boolean isPostLiked(WebElement likeButtonSvgElement) {
        String ariaLabelValue = likeButtonSvgElement.getAttribute("aria-label")
        if (ariaLabelValue == "Like") {
            return false
        } else if (ariaLabelValue == "Unlike") {
            return true
        } else {
            throw new IllegalArgumentException("Unexpected aria-label value; expected 'Like' or 'Unlike', received: $ariaLabelValue")
        }
    }

    /**
     * @return - webelement which will redirect the driver to the following post
     */
    private WebElement getNextPostButtonElement() {
        List<WebElement> nextElements = instaDriver.driver.findElements(By.xpath("//a[contains(text(), 'Next')]"))
        if (nextElements.size() == 0) {
            return null
        } else {
            return nextElements.first()
        }
    }
}
