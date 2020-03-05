package com.instabot

import com.instabot.operators.weboperations.userextractor.RelatedUsersUpdater
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

    private InstaWebDriver instaDriver

    @Autowired
    RelatedUsersUpdater relatedUsersUpdater

    def start(String loginUsername, String loginPassword, String masterUsername) throws InterruptedException {
        try {
            LOG.info("Start InstaBot execution")
            // TODO It's kind of weird that you instantiate the instaWebDriver here, then pass it as param and then just close it.
            // I would extract the following and followers here, close the connection ASAP and pass them as parameters to RelatedUsersUpdater
            instaDriver = new InstaWebDriver(loginUsername, loginPassword)
            relatedUsersUpdater.updateRelatedUsers(instaDriver, masterUsername)

            // other features here

            instaDriver.closeConnection()
        }
        catch (UsersLoadingException e) {
            LOG.error("Could not load all users", e)
            FileHandler.savePageSourceOnException(instaDriver)
            throw e
        }
    }
}