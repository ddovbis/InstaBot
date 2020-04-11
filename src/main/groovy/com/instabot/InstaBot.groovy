package com.instabot

import com.instabot.config.InstaBotConfig
import com.instabot.operations.reporters.RelatedUsersReporter
import com.instabot.operations.userextractors.relatedusers.RelatedUsersUpdater
import com.instabot.operations.userprocessors.relatedusers.RelatedUsersProcessor
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
    private RelatedUsersProcessor relatedUsersProcessor

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

            // update related users in database
            relatedUsersUpdater.updateRelatedUsers(masterUsername)

            // send related users report
            relatedUsersReporter.sendReport(masterUsername)

            // process related users (like/comment posts)
            relatedUsersProcessor.processRelatedUsers(masterUsername)

            // other features here

            // close InstaWebDriver connection at the end of the execution
            instaDriver.closeConnection()
        }
        catch (UsersLoadingException e) {
            LOG.error("Could not load all users", e)
            PageSourceSaver.savePageSourceOnException(instaDriver)
            throw e
        }
    }
}