package com.instabot

import com.instabot.utils.exceptions.user.UsersLoadingException
import com.instabot.utils.filehandler.FileHandler
import com.instabot.webdriver.InstaWebDriver
import com.instabot.weboperations.userextractor.RelatedUsersUpdater
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.ini4j.Wini
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

@Component
@DependsOn("loadSystemProperties")
class InstaBot {
    private static final Logger LOG = LogManager.getLogger(InstaBot.class)

    @Autowired
    private InstaWebDriver instaDriver
    @Autowired
    private RelatedUsersUpdater relatedUsersUpdater

    String masterUsername
    int relatedUsersUpdaterFrequency

    protected InstaBot() {
        initialize()
    }

    private void initialize() {
        LOG.info("Initialize InstaBot")
        String configurationFilePath = System.getProperty("instabot.conf.path")
        File configurationFile = new File(configurationFilePath)
        if (configurationFile == null) {
            throw new FileNotFoundException("Could not find insta-bot configuration file by path: $configurationFilePath")
        }

        Wini ini = new Wini(configurationFile)
        masterUsername = ini.get("related-users-updater", "master-username", String.class)
        relatedUsersUpdaterFrequency = ini.get("related-users-updater", "frequency", Integer.class)
    }

    void start() throws InterruptedException {
        try {
            LOG.info("Start InstaBot execution")
            relatedUsersUpdater.updateRelatedUsers(masterUsername, relatedUsersUpdaterFrequency)

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