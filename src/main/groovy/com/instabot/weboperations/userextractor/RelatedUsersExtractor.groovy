package com.instabot.weboperations.userextractor

import com.instabot.data.model.user.User
import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RelatedUsersExtractor {
    private final Logger LOG = LogManager.getLogger(RelatedUsersExtractor.class)

    @Autowired
    InstaWebDriver instaDriver

/**
 * Updates {@link com.instabot.data.model.user.User#name} and {@link com.instabot.data.model.user.User#isFollowed} or {@link com.instabot.data.model.user.User#isFollower} parameters
 * based on {@param relationToMasterUser}, for all {@link com.instabot.data.model.user.User}s found in {@param pageSource}
 *
 * @param extractedUserIdToUserMap - map containing userId as a key and user as a value
 * @param pageSource - page source with loaded {@param relationToMasterUser} users
 * @param masterUsername - user whose page is loaded in {@link InstaWebDriver}
 * @param relationToMasterUser - isFollower or isFollowed
 */
    void extractRelatedUsersFromPageSource(Map<String, User> extractedUserIdToUserMap, String pageSource, String masterUsername, String relationToMasterUser) {
        LOG.info("Extract loaded $relationToMasterUser users from page source")
        for (Element singleUserContainer : getSingleUserContainerList(pageSource)) {
            // extract username from link's text (ignore elements with images)
            Element userLink = singleUserContainer.select("a[href]:not(:has(img))").first()
            String username = userLink.text()
            // get userLink's "brother" element, which contains user's name (ignore elements containing title attribute)
            Element userLinkParent = userLink.parent().parent()
            String name = userLinkParent.select("div:not(:has([title]))").first().html()

            // TODO return a list, and update it separateley in a Collection of users; send the collection to be updated in dataservice at once (instead of updating each user)
            User user = new User(masterUsername, username, name)
            if (extractedUserIdToUserMap.containsKey(user.id)) {
                user = extractedUserIdToUserMap.get(user.id)
            } else {
                extractedUserIdToUserMap.put(user.id, user)
            }
            setUserRelationToMasterUser(user, relationToMasterUser)
        }
    }

    /**
     * @param pageSource - html page source containing an open "Following" or "Followers" users window
     * @return html list with all "Following" or "Followers" users
     */
    private Elements getSingleUserContainerList(String pageSource) {
        Document htmlDocument = Jsoup.parse(pageSource)
        // find first button (expected to be in the same div with the first user)
        Element firstButton = htmlDocument.select("div[role='dialog'] li button").first()
        // navigate 6 steps back through DOM tree to locate the scrolling element - list with all users
        Element usersContainer = firstButton.parent().parent().parent().parent().parent().parent()
        // extract the list containing information about each individual user
        return usersContainer.select("li")
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

}
