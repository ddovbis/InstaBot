package com.instabot

import com.instabot.weboperations.userextractor.RelatedUsersUpdater
import com.instabot.utils.exceptions.user.UsersLoadingException
import com.instabot.utils.filehandler.FileHandler
import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class InstaBot {
    private static final Logger LOG = LogManager.getLogger(InstaBot.class)

    @Autowired
    private InstaWebDriver instaDriver

    @Autowired
    RelatedUsersUpdater relatedUsersUpdater

    // TODO to be extracted from conf
    String masterUsername = "lina_alexandrean"

    def start() throws InterruptedException {
        try {
            LOG.info("Start InstaBot execution")
            // TODO It's kind of weird that you instantiate the instaWebDriver here, then pass it as param and then just close it.
            // I would extract the following and followers here, close the connection ASAP and pass them as parameters to RelatedUsersUpdater
            relatedUsersUpdater.updateRelatedUsers(masterUsername)

            // other features here

            // close InstaWebDriver connection at the end of the execution
            instaDriver.closeConnection()
        }
        catch (UsersLoadingException e) {
            LOG.error("Could not load all users", e)
            FileHandler.savePageSourceOnException(instaDriver)
            throw e
        }
    }
}