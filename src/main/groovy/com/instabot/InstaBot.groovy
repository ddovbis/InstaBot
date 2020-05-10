package com.instabot

import com.instabot.config.InstaBotConfig
import com.instabot.operations.relatedusersliker.RelatedUsersLiker
import com.instabot.operations.reporters.RelatedUsersReporter
import com.instabot.operations.userextractors.relatedusers.RelatedUsersUpdater
import com.instabot.utils.exceptions.user.UsersLoadingException
import com.instabot.utils.filehandler.PageSourceSaver
import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

@Component
@DependsOn("initializeInstaBotConfig")
class InstaBot {
    private static final Logger LOG = LogManager.getLogger(InstaBot.class)

    @Autowired
    private InstaBotConfig initializeInstaBotConfig
    @Autowired
    private InstaWebDriver instaDriver
    @Autowired
    private RelatedUsersUpdater relatedUsersUpdater
    @Autowired
    private RelatedUsersReporter relatedUsersReporter
    @Autowired
    private RelatedUsersLiker relatedUsersLiker

    private String masterUsername

    @Bean("initializeInstaBot")
    @DependsOn("initializeInstaBotConfig")
    private void initialize() {
        LOG.info("Initialize InstaBot")
        masterUsername = initializeInstaBotConfig.getIniFile().get("related-users", "master-username", String.class)
    }

    void start() throws InterruptedException {
        try {
            LOG.info("Start InstaBot execution")
            if (masterUsername == instaDriver.primaryUsername) {
                standardMode()
            } else {
                reportingMode()
            }
        }
        catch (UsersLoadingException e) {
            LOG.error("Could not load all users", e)
            PageSourceSaver.savePageSourceOnException(instaDriver)
            throw e
        } finally {
            LOG.info("All operations are finished; close InstaWebDriver connection...")
            instaDriver.closeConnection()
        }
    }

    private void standardMode() {
        // update related users in database
        relatedUsersUpdater.updateRelatedUsers(masterUsername)

        // send related users report
        relatedUsersReporter.sendReport(masterUsername)

        // like posts published by related users
        relatedUsersLiker.likeRelatedUsersPosts()

        // other features here
    }

    private void reportingMode() {
        LOG.info("InstaBot has been started in reporting-only mode (master username doesn't belong to the primary user logged into Instagram)")

        // update related users in database
        relatedUsersUpdater.updateRelatedUsers(masterUsername)

        // send related users report
        relatedUsersReporter.sendReport(masterUsername)
    }
}