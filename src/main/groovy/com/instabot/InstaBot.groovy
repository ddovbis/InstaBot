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

    def start(String loginUsername, String loginPassword) throws InterruptedException {
        try {
            LOG.info("Start InstaBot execution")
            instaDriver = new InstaWebDriver(loginUsername, loginPassword)
            relatedUsersUpdater.updateRelatedUsers(instaDriver, "lina_alexandrean")
            instaDriver.closeConnection()
        }
        catch (UsersLoadingException e) {
            LOG.error("Could not load all users", e)
            FileHandler.savePageSourceOnException(instaDriver)
            throw e
        }
    }
}