package com.instabot.operations.liker.relatedusers

import com.instabot.config.InstaBotConfig
import com.instabot.data.model.interaction.like.LikeInteraction
import com.instabot.data.model.user.User
import com.instabot.data.model.user.UserLabel
import com.instabot.data.services.interaction.like.LikeInteractionDataService
import com.instabot.data.services.primaryuser.PrimaryUserDataService
import com.instabot.data.services.user.UserDataService
import com.instabot.operations.helper.OperationsHelper
import com.instabot.operations.liker.relatedusers.helper.LikesProcessingBlockManager
import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class RelatedUsersLiker {
    private static final Logger LOG = LogManager.getLogger(RelatedUsersLiker.class)

    @Autowired
    private InstaBotConfig instaBotConfig
    @Autowired
    private UserDataService userDataService
    @Autowired
    private LikeInteractionDataService likeInteractionDataService
    @Autowired
    private PrimaryUserDataService primaryUserDataService
    @Autowired
    private OperationsHelper operationsHelper
    @Autowired
    private InstaWebDriver instaDriver
    @Autowired
    private LikesProcessingBlockManager likesProcessingBlockManager

    private int targetedNrOfLikesMin
    private int targetedNrOfLikesMax
    private int maxLikesPerHour

    private int targetSleepTimeAfterOpeningUserPage
    private int targetSleepTimeAfterMovingToNextPost
    private int targetSleepTimeAfterPerformingPostLike

    @Bean("initializeRelatedUsersLiker")
    private void initialize() {
        LOG.info("Initialize RelatedUsersLiker")

        targetedNrOfLikesMin = instaBotConfig.getIniFile().get("user-liker", "targeted-nr-of-likes-min", Integer.class)
        LOG.info("Min. targeted nr. of likes: $targetedNrOfLikesMin")

        targetedNrOfLikesMax = instaBotConfig.getIniFile().get("user-liker", "targeted-nr-of-likes-max", Integer.class)
        LOG.info("Max. targeted nr. of likes: $targetedNrOfLikesMax")

        maxLikesPerHour = instaBotConfig.getIniFile().get("user-liker", "max-likes-per-hour", Integer.class)
        LOG.info("Max. posts to be liked per hour: $maxLikesPerHour")

        setTimers()
    }

    /**
     * Sets how long should be the delay (sleep time) after
     * - opening a user page
     * - moving to the next user's post
     * - pressing on the 'like' button under the user's post
     */
    private void setTimers() {
        double avgLikesPerUser = (targetedNrOfLikesMin + targetedNrOfLikesMax) / 2
        LOG.debug("Avg. likes per user: $avgLikesPerUser")

        double avgUsersPerHour = maxLikesPerHour / avgLikesPerUser
        LOG.debug("Avg. users to be processed per hour: $avgUsersPerHour")

        double avgTimePerUser = 60 * 60 * 1000 / avgUsersPerHour
        LOG.debug("Avg. time to process one user: $avgTimePerUser ms")

        targetSleepTimeAfterOpeningUserPage = (avgTimePerUser * 0.2) as int
        LOG.info("Target sleep time after opening user page: $targetSleepTimeAfterOpeningUserPage ms")

        double avgTimeToProcessOnePost = (avgTimePerUser - targetSleepTimeAfterOpeningUserPage) / avgLikesPerUser
        LOG.debug("Avg. time to process one post: $avgTimeToProcessOnePost ms")

        targetSleepTimeAfterMovingToNextPost = (avgTimeToProcessOnePost * 0.7) as int
        LOG.info("Target sleep time after moving to the next post: $targetSleepTimeAfterMovingToNextPost ms")

        targetSleepTimeAfterPerformingPostLike = (avgTimeToProcessOnePost * 0.3) as int
        LOG.info("Target sleep time after performing post like: $targetSleepTimeAfterPerformingPostLike ms")
    }

    /**
     * Iterates thorough the Instagram pages of all users whose posts should be liked, opening the posts and clicking on the "like" button.
     */
    void likeRelatedUsersPosts() {
        LOG.info("Start processing post likes for users related to primary user: $instaDriver.primaryUsername")

        List<User> userToBeLikedList = userDataService.getAllToBeLikedByMasterUsername(instaDriver.primaryUsername)

        int totalLikesCount = likeInteractionDataService.countByPrimaryUsername(instaDriver.primaryUsername)
        LOG.info("Processed likes so far by primary user: $totalLikesCount")

        for (User userToBeLiked : userToBeLikedList) {
            boolean isPrimaryUserBlocked = likesProcessingBlockManager.blockPrimaryUserLikesProcessingIfNecessary()
            if (isPrimaryUserBlocked) {
                return
            }

            operationsHelper.goToUserPage(userToBeLiked.username)
            sleepAfterOpeningUserPage()
            likeUserPosts(userToBeLiked)
        }

        LOG.info("Processing post likes is finished")

        List<User> remainedUserToBeLikedList = userDataService.getAllToBeLikedByMasterUsername(instaDriver.primaryUsername)
        LOG.info("Liked users during the last iteration: ${userToBeLikedList.size() - remainedUserToBeLikedList.size()}")

        int newTotalLikedCount = likeInteractionDataService.countByPrimaryUsername(instaDriver.primaryUsername)
        LOG.info("Processed likes during the last iteration: ${newTotalLikedCount - totalLikesCount}")
    }

    private void likeUserPosts(User user) {
        LOG.info("Start processing post likes for user: $user.username")

        setTargetNrOfLikes(user)
        if (isFullyLiked(user)) {
            return
        }

        if (operationsHelper.userHasNoPosts()) {
            LOG.info("User has no posts")
            setAsFullyLiked(user)
            return
        }

        operationsHelper.openFirstPost(user.username)
        sleepAfterOpeningFirstPost()

        int postNr = 0
        do {
            LOG.info("Like post nr: ${++postNr}")
            WebElement likeButtonSvgElement = getLikeButtonSvgElement()
            if (isPostLiked(likeButtonSvgElement)) {
                LOG.info("Post has already been liked")
                setAsFullyLiked(user)
                return
            }

            performPostLike(likeButtonSvgElement)
            reportPostLike(user)
            sleepAfterPerformingPostLike()
            if (isFullyLiked(user)) {
                return
            }

            WebElement nextPostButtonElement = getNextPostButtonElement(user)
            if (nextPostButtonElement == null) {
                setAsFullyLiked(user)
                return
            }

            moveToNextPost(nextPostButtonElement)
            sleepAfterMovingToNextPost()
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
        userDataService.save(user.addLabel(UserLabel.FULLY_LIKED))
    }

    /**
     * @return - svg element contained in the "Like" button, which also can be used as a reference to click the button
     * Note: <svg> tag is used as a container for SVG graphics
     */
    private WebElement getLikeButtonSvgElement() {
        // case-insensitive selector; aria-label ends in "like" (Like or Unlike)
        By likeButtonSvgCssSelector = By.cssSelector("svg[aria-label*='like' i]")
        instaDriver.wait10sec.until(ExpectedConditions.visibilityOfElementLocated(likeButtonSvgCssSelector))
        return instaDriver.driver.findElement(likeButtonSvgCssSelector)
    }

    /**
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

    private boolean isFullyLiked(User user) {
        if (user.isFullyLiked()) {
            LOG.info("User $user.username is labeled as fully liked ($user.nrOfLikes out of $user.targetedNrOfLikes posts liked)")
            return true
        }
        return false
    }

    private void performPostLike(WebElement likeButtonSvgElement) {
        instaDriver.actions.moveToElement(likeButtonSvgElement).click().perform()
    }

    private void reportPostLike(User user) {
        reportPostLikeOnUser(user)
        saveLikeInteraction(user)
        reportPostLikeOnPrimaryUser()
        LOG.info("Post like processed successfully; total posts liked: $user.nrOfLikes out of $user.targetedNrOfLikes")
    }

    private void reportPostLikeOnUser(User user) {
        userDataService.save(user.incrementNrOfLikes())
    }

    private void saveLikeInteraction(User targetUser) {
        likeInteractionDataService.save(new LikeInteraction(instaDriver.primaryUsername, targetUser.username))
    }

    private void reportPostLikeOnPrimaryUser() {
        primaryUserDataService.save(instaDriver.getPrimaryUser().incrementNrOfLikes())
    }

    /**
     * @return - webelement which will redirect the driver to the following post
     */
    private WebElement getNextPostButtonElement(User user) {
        List<WebElement> nextElements = instaDriver.driver.findElements(By.xpath("//a[contains(text(), 'Next')]"))
        if (nextElements.size() == 0) {
            LOG.info("User $user.username has no more posts")
            return null
        } else {
            return nextElements.first()
        }
    }

    private void moveToNextPost(WebElement nextPostButtonElement) {
        LOG.debug("Move to the next post")
        operationsHelper.clickOnWebElement(nextPostButtonElement)
    }

    private void sleepAfterOpeningUserPage() {
        int sleepTime = getSleepTime(targetSleepTimeAfterOpeningUserPage, 0.35)
        LOG.debug("Sleep after opening user page: $sleepTime ms ")
        sleep(sleepTime)
    }

    private void sleepAfterOpeningFirstPost() {
        int sleepTime = getSleepTime(targetSleepTimeAfterMovingToNextPost, 0.25)
        LOG.debug("Sleep after opening first post: $sleepTime ms ")
        sleep(sleepTime)
    }

    private void sleepAfterPerformingPostLike() {
        int sleepTime = getSleepTime(targetSleepTimeAfterPerformingPostLike, 0.25)
        LOG.debug("Sleep after performing post like: $sleepTime ms ")
        sleep(sleepTime)
    }

    private void sleepAfterMovingToNextPost() {
        int sleepTime = getSleepTime(targetSleepTimeAfterMovingToNextPost, 0.25)
        LOG.debug("Sleep after moving to the next post: $sleepTime ms ")
        sleep(sleepTime)
    }

    private int getSleepTime(double targetSleepTime, double marginPercentage) {
        double margin = targetSleepTime * marginPercentage
        int minSleepTime = (targetSleepTime - margin) as int
        int maxSleepTime = (targetSleepTime + margin) as int
        return operationsHelper.getRandomInt(minSleepTime, maxSleepTime)
    }
}
